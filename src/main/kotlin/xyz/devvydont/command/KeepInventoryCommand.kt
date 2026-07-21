package xyz.devvydont.command

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.gamerules.GameRules
import xyz.devvydont.data.PlayerKeepInventoryData

/**
 * The keep inventory portion of the command tree: lets players view, set, and reset
 * their own personal keep inventory override, and lets privileged sources set the
 * override of others. A player without an override follows the server's game rule.
 */
object KeepInventoryCommand {

    private const val KEEP_INVENTORY_LITERAL = "keepinventory"
    private const val VALUE_ARGUMENT = "value"

    internal fun build(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal(KEEP_INVENTORY_LITERAL)
            .executes { ctx -> executeGetSelf(ctx) }
            .then(valueArgument().executes { ctx -> executeSetSelf(ctx) })
            .then(Commands.literal("get")
                .executes { ctx -> executeGetSelf(ctx) }
                .then(Commands.argument(ModCommands.TARGET_ARGUMENT, EntityArgument.player())
                    .executes { ctx -> executeGetOther(ctx) }))
            .then(Commands.literal("reset")
                .executes { ctx -> executeReset(ctx) })
            .then(Commands.literal("set")
                .then(valueArgument().executes { ctx -> executeSetSelf(ctx) })
                .then(Commands.argument(ModCommands.TARGET_ARGUMENT, EntityArgument.players())
                    .then(valueArgument().executes { ctx -> executeSetOther(ctx) })))
    }

    /**
     * Builds a fresh value argument node. Brigadier builders are mutable, so every
     * branch of the command tree must get its own instance rather than share one.
     */
    private fun valueArgument(): RequiredArgumentBuilder<CommandSourceStack, Boolean> {
        return Commands.argument(VALUE_ARGUMENT, BoolArgumentType.bool())
    }

    private fun enabledWord(value: Boolean): String {
        return if (value) "enabled" else "disabled"
    }

    private fun serverKeepInventory(player: ServerPlayer): Boolean {
        return player.level().gameRules[GameRules.KEEP_INVENTORY]
    }

    private fun describe(player: ServerPlayer): String {
        val override = PlayerKeepInventoryData.getPlayerKeepInventoryOverride(player)
        return if (override == null)
            "following the server setting (${enabledWord(serverKeepInventory(player))})"
        else
            enabledWord(override)
    }

    private fun reportKeepInventory(ctx: CommandContext<CommandSourceStack>, player: ServerPlayer): Int {
        ctx.source.sendSuccess({ Component.literal("${player.plainTextName}'s keep inventory is: ${describe(player)}") }, false)
        return 1
    }

    private fun executeGetSelf(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player
        if (player == null) {
            ctx.source.sendFailure(Component.literal("Only players can use this command without a target!"))
            return 0
        }

        return reportKeepInventory(ctx, player)
    }

    private fun executeGetOther(ctx: CommandContext<CommandSourceStack>): Int {
        return reportKeepInventory(ctx, EntityArgument.getPlayer(ctx, ModCommands.TARGET_ARGUMENT))
    }

    private fun executeSetSelf(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val value = BoolArgumentType.getBool(ctx, VALUE_ARGUMENT)

        PlayerKeepInventoryData.setPlayerKeepInventory(player, value)
        ctx.source.sendSuccess({ Component.literal("Set your personal keep inventory to ${enabledWord(value)}") }, false)
        return 1
    }

    private fun executeSetOther(ctx: CommandContext<CommandSourceStack>): Int {
        if (!ModCommands.canModifyOthers(ctx.source)) {
            ctx.source.sendFailure(Component.literal("You do not have permission to set other players' keep inventory."))
            return 0
        }

        val value = BoolArgumentType.getBool(ctx, VALUE_ARGUMENT)
        val targets = EntityArgument.getPlayers(ctx, ModCommands.TARGET_ARGUMENT)

        for (target in targets) {
            PlayerKeepInventoryData.setPlayerKeepInventory(target, value)
            target.sendSystemMessage(Component.literal("Your personal keep inventory was set to ${enabledWord(value)} by ${ctx.source.textName}"))
            ctx.source.sendSuccess({ Component.literal("Set ${target.plainTextName}'s keep inventory to ${enabledWord(value)}") }, true)
        }

        return targets.size
    }

    private fun executeReset(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException

        PlayerKeepInventoryData.clearPlayerKeepInventory(player)
        ctx.source.sendSuccess({ Component.literal("Your keep inventory now follows the server setting (${enabledWord(serverKeepInventory(player))}).") }, false)
        return 1
    }
}
