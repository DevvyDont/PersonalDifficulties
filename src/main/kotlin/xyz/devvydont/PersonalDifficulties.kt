package xyz.devvydont

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.world.Difficulty
import org.slf4j.LoggerFactory
import xyz.devvydont.command.DifficultyCommand
import xyz.devvydont.data.PlayerDifficultyData

object PersonalDifficulties : ModInitializer {
	val MOD_ID = "personal_difficulties"
	private val logger = LoggerFactory.getLogger("personal-difficulties")

	override fun onInitialize() {

		// Register the player difficulty data attachment for persistence.
		PlayerDifficultyData.register()
		logger.info("Registered difficulty persistence registry")

		// Register the command in order to modify player difficulties.
		DifficultyCommand.register()
		logger.info("Registered /difficulty command")

		// Set the server difficulty to hard for ideal game mechanic purposes.
		ServerLifecycleEvents.SERVER_STARTED.register(ServerLifecycleEvents.ServerStarted { server ->
			server.setDifficulty(Difficulty.HARD, true)
			logger.info("Server difficulty set to hard for ideal game mechanic behavior. This does not affect the damage people take!")
		})

	}
}