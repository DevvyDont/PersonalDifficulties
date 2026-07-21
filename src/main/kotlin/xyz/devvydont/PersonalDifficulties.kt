package xyz.devvydont

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.world.Difficulty
import org.slf4j.LoggerFactory
import xyz.devvydont.command.ModCommands
import xyz.devvydont.data.PlayerDifficultyData
import xyz.devvydont.data.PlayerKeepInventoryData

object PersonalDifficulties : ModInitializer {

	const val MOD_ID = "personal_difficulties"

	private val logger = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {

		// Register the player data attachments for persistence.
		PlayerDifficultyData.register()
		PlayerKeepInventoryData.register()
		logger.info("Registered player data attachments")

		// Register the command tree used to view and modify personal settings.
		ModCommands.register()
		logger.info("Registered /personaldifficulty command")

		// Set the server difficulty to hard for ideal game mechanic purposes.
		ServerLifecycleEvents.SERVER_STARTED.register { server ->
			server.setDifficulty(Difficulty.HARD, true)
			logger.info("Server difficulty set to hard for ideal game mechanic behavior. This does not affect the damage people take!")
		}
	}
}
