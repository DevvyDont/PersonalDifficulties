package xyz.devvydont.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Difficulty
import xyz.devvydont.data.PlayerDifficultyData
import xyz.devvydont.message.SettingsMessages
import java.util.concurrent.CompletableFuture

/**
 * The difficulty portion of the command tree: lets players view, set, and reset their
 * own personal difficulty, and lets privileged sources set the difficulty of others.
 */
object DifficultyCommand {

    private const val DIFFICULTY_ARGUMENT = "difficulty"

    /**
     * Attaches the difficulty nodes to the root of the command tree. Difficulty is the
     * mod's original feature, so it owns the root-level get/set/reset grammar. The bare
     * root command itself belongs to ModCommands (it opens the settings menu).
     */
    internal fun appendTo(builder: LiteralArgumentBuilder<CommandSourceStack>): LiteralArgumentBuilder<CommandSourceStack> {
        return builder
            .then(difficultyArgument().executes { ctx -> executeSetSelf(ctx) })
            .then(Commands.literal("get")
                .executes { ctx -> executeGetSelf(ctx) }
                .then(Commands.argument(ModCommands.TARGET_ARGUMENT, EntityArgument.player())
                    .executes { ctx -> executeGetOther(ctx) }))
            .then(Commands.literal("reset")
                .executes { ctx -> executeReset(ctx) })
            .then(Commands.literal("set")
                .then(difficultyArgument().executes { ctx -> executeSetSelf(ctx) })
                .then(Commands.argument(ModCommands.TARGET_ARGUMENT, EntityArgument.players())
                    .then(difficultyArgument().executes { ctx -> executeSetOther(ctx) })))
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
        return reportDifficulty(ctx, EntityArgument.getPlayer(ctx, ModCommands.TARGET_ARGUMENT))
    }

    private fun executeSetSelf(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.playerOrException
        val difficulty = parseDifficultyArgument(ctx) ?: return 0

        PlayerDifficultyData.setPlayerDifficulty(player, difficulty)
        ctx.source.sendSuccess({ SettingsMessages.difficultySet(difficulty) }, false)
        return 1
    }

    private fun executeSetOther(ctx: CommandContext<CommandSourceStack>): Int {
        if (!ModCommands.canModifyOthers(ctx.source)) {
            ctx.source.sendFailure(Component.literal("You do not have permission to set other players' difficulties."))
            return 0
        }

        val difficulty = parseDifficultyArgument(ctx) ?: return 0
        val targets = EntityArgument.getPlayers(ctx, ModCommands.TARGET_ARGUMENT)

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
