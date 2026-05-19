package xyz.devvydont.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.util.TriState
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.PermissionLevel
import net.minecraft.world.Difficulty
import xyz.devvydont.PersonalDifficulties.MOD_ID
import xyz.devvydont.data.PlayerDifficultyData
import java.util.concurrent.CompletableFuture

object DifficultyCommand {

    private val DIFFICULTY_NAMES = listOf("peaceful", "easy", "normal", "hard")

    fun register() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher, _, _ ->
            dispatcher.register(
                Commands.literal("personaldifficulty")
                    .executes { ctx -> executeGet(ctx) }
                    .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes { ctx -> executeGet(ctx) })
                        .executes { ctx -> executeGet(ctx) })
                    .then(Commands.literal("reset")
                        .executes { ctx -> executeReset(ctx, ctx.source.playerOrException) })
                    .then(Commands.literal("set")
                        .then(Commands.argument("difficulty", StringArgumentType.word())
                            .suggests(this::difficultySuggestions)
                            .executes { ctx -> executeSetSelf(ctx) })
                        .then(Commands.argument("target", EntityArgument.players())
                            .then(Commands.argument("difficulty", StringArgumentType.word())
                                .suggests(this::difficultySuggestions)
                                .executes { ctx -> executeSetOther(ctx) })))
            )
        })
    }

    private fun difficultySuggestions(ctx: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        DIFFICULTY_NAMES.forEach { d -> builder.suggest(d) }
        return builder.buildFuture()
    }

    private fun parseDifficulty(name: String): Difficulty? {
        return DIFFICULTY_NAMES.indexOf(name.lowercase()).let { idx ->
            when (idx) {
                0 -> Difficulty.PEACEFUL
                1 -> Difficulty.EASY
                2 -> Difficulty.NORMAL
                3 -> Difficulty.HARD
                else -> null
            }
        }
    }

    private fun executeGet(ctx: CommandContext<CommandSourceStack>): Int {
        val player = try {
            EntityArgument.getPlayer(ctx, "target")
        } catch (e: IllegalArgumentException) {
            // No target argument, try to get player from command source
            try {
                ctx.source.playerOrException
            } catch (e: Exception) {
                ctx.source.sendFailure(Component.literal("Only players can use this command with no arguments!"))
                return 0
            }
        }

        val difficulty = PlayerDifficultyData.getPlayerDifficulty(player)
        ctx.source.sendSuccess({ Component.literal("${player.plainTextName}'s difficulty is: ${difficulty.name}")}, false)
        return 1
    }

    private fun executeSetSelf(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val diffName = StringArgumentType.getString(ctx, "difficulty")
        val difficulty = parseDifficulty(diffName) ?: run {
            ctx.source.sendFailure(Component.literal("Invalid difficulty: $diffName"))
            return 0
        }

        PlayerDifficultyData.setPlayerDifficulty(player, difficulty)
        // Optional: mark unsaved, see persistence note
        ctx.source.sendSuccess({ Component.literal("Set your personal difficulty to ${difficulty.name.lowercase()}") }, false)
        return 1
    }

    private fun executeSetOther(ctx: CommandContext<CommandSourceStack>): Int {
        // Permission check: require at least permission level 2 (operators)
        val hasPerm = ctx.source.checkPermission(Identifier.fromNamespaceAndPath(MOD_ID, "modifyothers")) == TriState.TRUE
        val hasOp = ctx.source.player?.permissionContext?.permissionLevel()?.isEqualOrHigherThan(PermissionLevel.ADMINS) ?: false
        if (!(hasPerm || hasOp)) {
            ctx.source.sendFailure(Component.literal("You do not have permission to set other players' difficulties."))
            return 0
        }

        val targets = EntityArgument.getPlayers(ctx, "target")
        val diffName = StringArgumentType.getString(ctx, "difficulty")
        val difficulty = parseDifficulty(diffName) ?: run {
            ctx.source.sendFailure(Component.literal("Invalid difficulty: $diffName"))
            return 0
        }

        for (player in targets) {
            PlayerDifficultyData.setPlayerDifficulty(player, difficulty)
            player.sendSystemMessage(Component.literal("Your personal difficulty was set to ${difficulty.name.lowercase()} by ${ctx.source.textName}"))
            ctx.source.sendSuccess({ Component.literal("Set ${player.plainTextName}'s difficulty to ${difficulty.name.lowercase()}") }, true)
        }

        return 1
    }

    private fun executeReset(ctx: CommandContext<CommandSourceStack>, player: ServerPlayer): Int {
        // Reset to default: we can treat reset as setting to NORMAL or removing attachment (your design)
        PlayerDifficultyData.setPlayerDifficulty(player, Difficulty.NORMAL)
        ctx.source.sendSuccess({ Component.literal("Reset your personal difficulty to normal.") }, false)
        return 1
    }
}