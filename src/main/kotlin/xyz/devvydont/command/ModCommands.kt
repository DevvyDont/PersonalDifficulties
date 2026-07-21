package xyz.devvydont.command

import com.mojang.brigadier.context.CommandContext
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.players.NameAndId
import xyz.devvydont.PersonalDifficulties.MOD_ID
import xyz.devvydont.menu.SettingsMenu

/**
 * Registers the mod's command tree. Feature commands contribute their own subtrees:
 * difficulty occupies the root level, and other personal settings live under their
 * own literals (e.g. /personaldifficulty keepinventory). The bare command opens the
 * settings menu for players; consoles must use the subcommands.
 */
object ModCommands {

    private const val ROOT_COMMAND = "personaldifficulty"
    private const val ROOT_ALIAS = "pd"
    private const val MODIFY_OTHERS_PERMISSION = "$MOD_ID.modifyothers"

    internal const val TARGET_ARGUMENT = "target"

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            val root = dispatcher.register(
                DifficultyCommand.appendTo(
                    Commands.literal(ROOT_COMMAND)
                        .executes { ctx -> executeOpenMenu(ctx) })
                    .then(KeepInventoryCommand.build())
            )

            // A Brigadier redirect only forwards when arguments follow it, so the alias
            // needs its own executes for the bare command.
            dispatcher.register(Commands.literal(ROOT_ALIAS)
                .executes { ctx -> executeOpenMenu(ctx) }
                .redirect(root))
        }
    }

    private fun executeOpenMenu(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.player
        if (player == null) {
            ctx.source.sendFailure(Component.literal("Only players can open the settings menu; use the subcommands instead."))
            return 0
        }

        SettingsMenu.open(player)
        return 1
    }

    /**
     * A source may modify other players' settings if it has the permission node
     * (LuckPerms and similar via fabric-permissions-api) or is a server operator.
     */
    internal fun canModifyOthers(source: CommandSourceStack): Boolean {
        if (Permissions.check(source, MODIFY_OTHERS_PERMISSION))
            return true

        val player = source.player ?: return false
        return source.server.playerList.isOp(NameAndId(player.gameProfile))
    }
}
