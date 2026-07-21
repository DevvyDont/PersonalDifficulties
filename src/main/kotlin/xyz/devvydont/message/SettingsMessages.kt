package xyz.devvydont.message

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Difficulty
import xyz.devvydont.data.PlayerKeepInventoryData

/**
 * Shared user-facing wording for personal settings, used by both the command handlers
 * and the settings menu so the two surfaces always describe state identically.
 */
object SettingsMessages {

    fun enabledWord(value: Boolean): String {
        return if (value) "enabled" else "disabled"
    }

    /**
     * Human-readable description of a player's keep inventory state, accounting for the
     * tri-state nature of the setting.
     */
    fun describeKeepInventory(player: ServerPlayer): String {
        val override = PlayerKeepInventoryData.getPlayerKeepInventoryOverride(player)
        return if (override == null)
            "following the server setting (${enabledWord(PlayerKeepInventoryData.getServerKeepInventory(player))})"
        else
            enabledWord(override)
    }

    fun difficultySet(difficulty: Difficulty): Component {
        return Component.literal("Set your personal difficulty to ${difficulty.serializedName}")
    }

    fun keepInventorySet(value: Boolean): Component {
        return Component.literal("Set your personal keep inventory to ${enabledWord(value)}")
    }

    fun keepInventoryFollowsServer(player: ServerPlayer): Component {
        return Component.literal("Your keep inventory now follows the server setting (${enabledWord(PlayerKeepInventoryData.getServerKeepInventory(player))}).")
    }
}
