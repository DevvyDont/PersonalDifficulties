package xyz.devvydont.data

import com.mojang.serialization.Codec
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.gamerules.GameRules
import xyz.devvydont.PersonalDifficulties

/**
 * Owns the persistence of every player's personal keep inventory override. Unlike difficulty,
 * this setting is tri-state: a player either has an explicit override (true/false) or no
 * override at all, in which case the server's keep inventory game rule applies.
 */
object PlayerKeepInventoryData {

    private const val ATTACHMENT_PATH = "keep_inventory"

    private lateinit var keepInventoryAttachment: AttachmentType<Boolean>

    fun register() {
        keepInventoryAttachment = AttachmentRegistry.create<Boolean>(
            Identifier.fromNamespaceAndPath(PersonalDifficulties.MOD_ID, ATTACHMENT_PATH)
        ) { builder -> builder.persistent(Codec.BOOL).copyOnDeath() }
    }

    /**
     * The player's personal override, or null when the player follows the server's game rule.
     */
    fun getPlayerKeepInventoryOverride(player: Player): Boolean? {
        return player.getAttached(keepInventoryAttachment)
    }

    /**
     * The keep inventory value that should apply to this player, given the value the server
     * would have used. This is what the death/respawn mixins consult.
     */
    fun getEffectiveKeepInventory(player: Player, serverValue: Boolean): Boolean {
        return player.getAttachedOrElse(keepInventoryAttachment, serverValue)
    }

    /**
     * The value of the server's keep inventory game rule, i.e. what applies to players
     * without a personal override.
     */
    fun getServerKeepInventory(player: ServerPlayer): Boolean {
        return player.level().gameRules[GameRules.KEEP_INVENTORY]
    }

    fun setPlayerKeepInventory(player: Player, keepInventory: Boolean) {
        player.setAttached(keepInventoryAttachment, keepInventory)
    }

    fun clearPlayerKeepInventory(player: Player) {
        player.removeAttached(keepInventoryAttachment)
    }
}
