package xyz.devvydont.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.players.NameAndId
import net.minecraft.world.Difficulty
import xyz.devvydont.PersonalDifficulties.MOD_ID
import xyz.devvydont.data.PlayerDifficultyData
import java.util.concurrent.CompletableFuture

/**
 * The /personaldifficulty command: lets players view, set, and reset their own personal
 * difficulty, and lets privileged sources set the difficulty of other players.
 */
object DifficultyCommand {

    private const val COMMAND_NAME = "personaldifficulty"
    private const val DIFFICULTY_ARGUMENT = "difficulty"
    private const val TARGET_ARGUMENT = "target"
    private const val MODIFY_OTHERS_PERMISSION = "$MOD_ID.modifyothers"

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                Commands.literal(COMMAND_NAME)
                    .executes { ctx -> executeGetSelf(ctx) }
                    .then(difficultyArgument().executes { ctx -> executeSetSelf(ctx) })
                    .then(Commands.literal("get")
                        .executes { ctx -> executeGetSelf(ctx) }
                        .then(Commands.argument(TARGET_ARGUMENT, EntityArgument.player())
                            .executes { ctx -> executeGetOther(ctx) }))
                    .then(Commands.literal("reset")
                        .executes { ctx -> executeReset(ctx) })
                    .then(Commands.literal("set")
                        .then(difficultyArgument().executes { ctx -> executeSetSelf(ctx) })
                        .then(Commands.argument(TARGET_ARGUMENT, EntityArgument.players())
                            .then(difficultyArgument().executes { ctx -> executeSetOther(ctx) })))
            )
        }
    }

    /**
     * Builds a fresh difficulty argument node. Brigadier builders are mutable, so every
     * branch of the command tree must get its own instance rather than share one.
     */
    private fun difficultyArgument(): RequiredArgumentBuilder<CommandSourceStack, String> {
        return Commands.argument(DIFFICULTY_ARGUMENT, StringArgumentType.word())
            .suggests { _, builder -> suggestDifficulties(builder) }
    }

    private fun suggestDifficulties(builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        Difficulty.entries.forEach { difficulty -> builder.suggest(difficulty.serializedName) }
        return builder.buildFuture()
    }

    private fun parseDifficulty(name: String): Difficulty? {
        return Difficulty.entries.firstOrNull { difficulty ->
            difficulty.serializedName.equals(name, ignoreCase = true)
        }
    }

    /**
     * Reads and parses the difficulty argument, sending a failure message to the source
     * when the value is not a valid difficulty name.
     */
    private fun parseDifficultyArgument(ctx: CommandContext<CommandSourceStack>): Difficulty? {
        val name = StringArgumentType.getString(ctx, DIFFICULTY_ARGUMENT)
        val difficulty = parseDifficulty(name)
        if (difficulty == null)
            ctx.source.sendFailure(Component.literal("Invalid difficulty: $name"))
        return difficulty
    }

    /**
     * A source may modify other players' difficulties if it has the permission node
     * (LuckPerms and similar via fabric-permissions-api) or is a server operator.
     */
    private fun canModifyOthers(source: CommandSourceStack): Boolean {
        if (Permissions.check(source, MODIFY_OTHERS_PERMISSION))
            return true

        val player = source.player ?: return false
        return source.server.playerList.isOp(NameAndId(player.gameProfile))
    }

    private fun reportDifficulty(ctx: CommandContext<CommandSourceStack>, player: ServerPlayer): Int {
        val difficulty = PlayerDifficultyData.getPlayerDifficulty(player)
        ctx.source.sendSuccess({ Component.literal("${player.plainTextName}'s difficulty is: ${difficulty.serializedName}") }, false)
        return 1
    }

    private fun executeGetSelf(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player
        if (player == null) {
            ctx.source.sendFailure(Component.literal("Only players can use this command without a target!"))
            return 0
        }

        return reportDifficulty(ctx, player)
    }

    private fun executeGetOther(ctx: CommandContext<CommandSourceStack>): Int {
        return reportDifficulty(ctx, EntityArgument.getPlayer(ctx, TARGET_ARGUMENT))
    }

    private fun executeSetSelf(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val difficulty = parseDifficultyArgument(ctx) ?: return 0

        PlayerDifficultyData.setPlayerDifficulty(player, difficulty)
        ctx.source.sendSuccess({ Component.literal("Set your personal difficulty to ${difficulty.serializedName}") }, false)
        return 1
    }

    private fun executeSetOther(ctx: CommandContext<CommandSourceStack>): Int {
        if (!canModifyOthers(ctx.source)) {
            ctx.source.sendFailure(Component.literal("You do not have permission to set other players' difficulties."))
            return 0
        }

        val difficulty = parseDifficultyArgument(ctx) ?: return 0
        val targets = EntityArgument.getPlayers(ctx, TARGET_ARGUMENT)

        for (target in targets) {
            PlayerDifficultyData.setPlayerDifficulty(target, difficulty)
            target.sendSystemMessage(Component.literal("Your personal difficulty was set to ${difficulty.serializedName} by ${ctx.source.textName}"))
            ctx.source.sendSuccess({ Component.literal("Set ${target.plainTextName}'s difficulty to ${difficulty.serializedName}") }, true)
        }

        return targets.size
    }

    private fun executeReset(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val default = PlayerDifficultyData.DEFAULT_DIFFICULTY

        PlayerDifficultyData.setPlayerDifficulty(player, default)
        ctx.source.sendSuccess({ Component.literal("Reset your personal difficulty to ${default.serializedName}.") }, false)
        return 1
    }
}
