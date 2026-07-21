package xyz.devvydont.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.devvydont.data.PlayerKeepInventoryData;

/**
 * This mixin hijacks into death drop logic to override where the keep inventory
 * game rule is read to use our mod's per-player setting rather than relying on
 * the server's game rule. Covers both the inventory drop (dropEquipment) and the
 * experience drop (getBaseExperienceReward). The dying player still has their
 * attachments at this point, so reading from {@code this} is safe.
 */
@Mixin(Player.class)
public class PlayerDeathDropsMixin {

    @Redirect(
            method = {"dropEquipment", "getBaseExperienceReward"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"
            )
    )
    private Object redirectKeepInventory(GameRules gameRules, GameRule<Boolean> rule) {
        var player = (Player) (Object) this;
        var serverValue = gameRules.get(rule);
        return PlayerKeepInventoryData.INSTANCE.getEffectiveKeepInventory(player, serverValue);
    }
}
