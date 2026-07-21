package xyz.devvydont.data

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.minecraft.resources.Identifier
import net.minecraft.world.Difficulty
import net.minecraft.world.entity.player.Player
import xyz.devvydont.PersonalDifficulties

/**
 * Owns the persistence of every player's personal difficulty. All reads and writes
 * of a player's difficulty must go through this object; the backing attachment is
 * an implementation detail.
 */
object PlayerDifficultyData {

    val DEFAULT_DIFFICULTY: Difficulty = Difficulty.NORMAL

    private const val ATTACHMENT_PATH = "difficulty"

    private lateinit var difficultyAttachment: AttachmentType<Difficulty>

    fun register() {
        difficultyAttachment = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath(PersonalDifficulties.MOD_ID, ATTACHMENT_PATH),
            Difficulty.CODEC
        )
    }

    fun getPlayerDifficulty(player: Player): Difficulty {
        return player.getAttachedOrElse(difficultyAttachment, DEFAULT_DIFFICULTY)
    }

    fun setPlayerDifficulty(player: Player, difficulty: Difficulty) {
        player.setAttached(difficultyAttachment, difficulty)
    }
}
