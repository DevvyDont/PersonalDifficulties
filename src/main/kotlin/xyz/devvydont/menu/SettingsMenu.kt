package xyz.devvydont.menu

import net.minecraft.ChatFormatting
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.Difficulty
import net.minecraft.world.SimpleContainer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.item.component.TooltipDisplay
import xyz.devvydont.data.PlayerDifficultyData
import xyz.devvydont.data.PlayerKeepInventoryData
import xyz.devvydont.message.SettingsMessages

/**
 * A server-side chest UI for viewing and changing personal settings, fully compatible
 * with vanilla clients. The chest slots act as buttons: clicks are intercepted and
 * cancelled server-side, so no item ever actually moves. Layout (9x3):
 *
 *  row 0: filler, with the info head in the center (slot 4)
 *  row 1: difficulty buttons, evenly spaced (slots 10, 12, 14, 16)
 *  row 2: filler, with the keep inventory toggle in the center (slot 22)
 */
class SettingsMenu private constructor(
    containerId: Int,
    playerInventory: Inventory,
    private val player: ServerPlayer,
) : ChestMenu(MenuType.GENERIC_9x3, containerId, playerInventory, SimpleContainer(CONTAINER_SIZE), CONTAINER_ROWS) {

    companion object {

        private const val CONTAINER_SIZE = 27
        private const val CONTAINER_ROWS = 3
        private const val INFO_SLOT = 4
        private const val KEEP_INVENTORY_SLOT = 22

        private val DIFFICULTY_SLOTS = mapOf(
            10 to Difficulty.PEACEFUL,
            12 to Difficulty.EASY,
            14 to Difficulty.NORMAL,
            16 to Difficulty.HARD,
        )

        private val TITLE: Component = Component.literal("Personal Settings")

        private const val SOUND_VOLUME = 0.5f
        private const val PITCH_SELECT = 1.0f
        private const val PITCH_ALREADY_SELECTED = 0.6f
        private const val PITCH_KEEP_INVENTORY_ENABLED = 1.2f
        private const val PITCH_KEEP_INVENTORY_DISABLED = 0.8f
        private const val PITCH_KEEP_INVENTORY_SERVER = 1.0f

        fun open(player: ServerPlayer) {
            player.openMenu(SimpleMenuProvider({ containerId, playerInventory, _ ->
                SettingsMenu(containerId, playerInventory, player)
            }, TITLE))
        }
    }

    init {
        refresh()
    }

    override fun clicked(slotId: Int, button: Int, input: ContainerInput, clickingPlayer: Player) {
        // Deliberately never calls super: this menu is a control panel and items must
        // never move, whether in the chest area or the player's own inventory.
        handleClick(slotId, input)
        sendAllDataToRemote()
    }

    override fun quickMoveStack(quickMovingPlayer: Player, index: Int): ItemStack {
        return ItemStack.EMPTY
    }

    override fun canTakeItemForPickAll(stack: ItemStack, slot: Slot): Boolean {
        return false
    }

    override fun canDragTo(slot: Slot): Boolean {
        return false
    }

    private fun handleClick(slotId: Int, input: ContainerInput) {
        if (input != ContainerInput.PICKUP && input != ContainerInput.QUICK_MOVE)
            return

        val difficulty = DIFFICULTY_SLOTS[slotId]
        if (difficulty != null) {
            selectDifficulty(difficulty)
            return
        }

        if (slotId == KEEP_INVENTORY_SLOT)
            cycleKeepInventory()
    }

    private fun selectDifficulty(difficulty: Difficulty) {
        if (PlayerDifficultyData.getPlayerDifficulty(player) == difficulty) {
            playSound(SoundEvents.NOTE_BLOCK_BASS, PITCH_ALREADY_SELECTED)
            return
        }

        PlayerDifficultyData.setPlayerDifficulty(player, difficulty)
        player.sendSystemMessage(SettingsMessages.difficultySet(difficulty))
        playSound(SoundEvents.UI_BUTTON_CLICK, PITCH_SELECT)
        refresh()
    }

    /**
     * Cycles the tri-state override: enabled -> disabled -> follow server -> enabled.
     */
    private fun cycleKeepInventory() {
        when (PlayerKeepInventoryData.getPlayerKeepInventoryOverride(player)) {
            true -> {
                PlayerKeepInventoryData.setPlayerKeepInventory(player, false)
                player.sendSystemMessage(SettingsMessages.keepInventorySet(false))
                playSound(SoundEvents.UI_BUTTON_CLICK, PITCH_KEEP_INVENTORY_DISABLED)
            }
            false -> {
                PlayerKeepInventoryData.clearPlayerKeepInventory(player)
                player.sendSystemMessage(SettingsMessages.keepInventoryFollowsServer(player))
                playSound(SoundEvents.UI_BUTTON_CLICK, PITCH_KEEP_INVENTORY_SERVER)
            }
            null -> {
                PlayerKeepInventoryData.setPlayerKeepInventory(player, true)
                player.sendSystemMessage(SettingsMessages.keepInventorySet(true))
                playSound(SoundEvents.UI_BUTTON_CLICK, PITCH_KEEP_INVENTORY_ENABLED)
            }
        }
        refresh()
    }

    private fun playSound(sound: Holder<SoundEvent>, pitch: Float) {
        player.connection.send(
            ClientboundSoundPacket(sound, SoundSource.UI, player.x, player.y, player.z, SOUND_VOLUME, pitch, player.random.nextLong())
        )
    }

    private fun refresh() {
        val selected = PlayerDifficultyData.getPlayerDifficulty(player)

        for (slot in 0 until CONTAINER_SIZE)
            container.setItem(slot, fillerItem())

        container.setItem(INFO_SLOT, infoItem())
        DIFFICULTY_SLOTS.forEach { (slot, difficulty) ->
            container.setItem(slot, difficultyButton(difficulty, difficulty == selected))
        }
        container.setItem(KEEP_INVENTORY_SLOT, keepInventoryButton())
    }

    private fun fillerItem(): ItemStack {
        val stack = ItemStack(Items.STAINED_GLASS_PANE.black())
        stack.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay(true, linkedSetOf()))
        return stack
    }

    private fun infoItem(): ItemStack {
        val stack = buildButton(
            Items.PLAYER_HEAD,
            label("${player.plainTextName}'s Settings", ChatFormatting.GOLD),
            listOf(
                loreLine("Difficulty: ${PlayerDifficultyData.getPlayerDifficulty(player).serializedName}"),
                loreLine("Keep inventory: ${SettingsMessages.describeKeepInventory(player)}"),
            ),
        )
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(player.gameProfile))
        return stack
    }

    private fun difficultyButton(difficulty: Difficulty, selected: Boolean): ItemStack {
        val (item, color, description) = when (difficulty) {
            Difficulty.PEACEFUL -> Triple(Items.FEATHER, ChatFormatting.GREEN, "Hostile mobs ignore you and your health regenerates.")
            Difficulty.EASY -> Triple(Items.WOODEN_SWORD, ChatFormatting.YELLOW, "Take reduced damage and hunger drains slowly.")
            Difficulty.NORMAL -> Triple(Items.IRON_SWORD, ChatFormatting.GOLD, "The standard survival experience.")
            Difficulty.HARD -> Triple(Items.NETHERITE_SWORD, ChatFormatting.RED, "Take increased damage and zombies can break doors.")
        }

        val name = difficulty.serializedName.replaceFirstChar { it.uppercase() }
        val status =
            if (selected) loreLine("Currently selected", ChatFormatting.GREEN)
            else loreLine("Click to select", ChatFormatting.YELLOW)

        val stack = buildButton(item, label(name, color), listOf(loreLine(description), status))
        if (selected)
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
        return stack
    }

    private fun keepInventoryButton(): ItemStack {
        val override = PlayerKeepInventoryData.getPlayerKeepInventoryOverride(player)
        val (item, color, state) = when (override) {
            true -> Triple(Items.TOTEM_OF_UNDYING, ChatFormatting.GREEN, "Enabled")
            false -> Triple(Items.BONE, ChatFormatting.RED, "Disabled")
            null -> Triple(
                Items.COMPASS,
                ChatFormatting.AQUA,
                "Server Default (${SettingsMessages.enabledWord(PlayerKeepInventoryData.getServerKeepInventory(player))})",
            )
        }

        return buildButton(
            item,
            label("Keep Inventory: $state", color),
            listOf(
                loreLine("Whether you keep your items and XP when you die."),
                loreLine("Server default follows the keep inventory game rule."),
                loreLine("Click to cycle", ChatFormatting.YELLOW),
            ),
        )
    }

    private fun buildButton(item: Item, name: Component, lore: List<Component>): ItemStack {
        val stack = ItemStack(item)
        stack.set(DataComponents.CUSTOM_NAME, name)
        stack.set(DataComponents.LORE, ItemLore(lore))
        stack.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.ATTRIBUTE_MODIFIERS, true))
        return stack
    }

    private fun label(text: String, color: ChatFormatting): Component {
        return Component.literal(text).withStyle { style -> style.withItalic(false).withColor(color) }
    }

    private fun loreLine(text: String, color: ChatFormatting = ChatFormatting.GRAY): Component {
        return Component.literal(text).withStyle { style -> style.withItalic(false).withColor(color) }
    }
}
