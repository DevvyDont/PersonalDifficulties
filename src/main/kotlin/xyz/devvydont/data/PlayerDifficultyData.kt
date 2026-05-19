package xyz.devvydont.data

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Difficulty
import net.minecraft.world.entity.player.Player
import xyz.devvydont.PersonalDifficulties


object PlayerDifficultyData {

    private lateinit var difficultyAttachment: AttachmentType<Difficulty>

    fun register() {
        difficultyAttachment = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath(PersonalDifficulties.MOD_ID, "difficulty"),
        Difficulty.CODEC
        )
    }

    fun getPlayerDifficulty(player: Player): Difficulty {
        return player.getAttachedOrElse(difficultyAttachment, Difficulty.NORMAL)
    }

    fun setPlayerDifficulty(player: Player, difficulty: Difficulty) {
        player.setAttached(difficultyAttachment, difficulty)
    }

}