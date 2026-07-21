package xyz.devvydont.command

import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.server.players.NameAndId
import xyz.devvydont.PersonalDifficulties.MOD_ID

/**
 * Registers the mod's command tree. Feature commands contribute their own subtrees:
 * difficulty occupies the root level, and other personal settings live under their
 * own literals (e.g. /personaldifficulty keepinventory).
 */
object ModCommands {

    private const val ROOT_COMMAND = "personaldifficulty"
    private const val ROOT_ALIAS = "pd"
    private const val MODIFY_OTHERS_PERMISSION = "$MOD_ID.modifyothers"

    internal const val TARGET_ARGUMENT = "target"

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            val root = dispatcher.register(
                DifficultyCommand.appendTo(Commands.literal(ROOT_COMMAND))
                    .then(KeepInventoryCommand.build())
            )

            // A Brigadier redirect only forwards when arguments follow it, so the alias
            // needs its own executes for the bare command.
            dispatcher.register(Commands.literal(ROOT_ALIAS)
                .executes { ctx -> DifficultyCommand.executeGetSelf(ctx) }
                .redirect(root))
        }
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
