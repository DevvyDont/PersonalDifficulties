package xyz.devvydont.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.devvydont.data.PlayerKeepInventoryData;

/**
 * This mixin hijacks into respawn logic to override where the keep inventory game
 * rule is read to use our mod's per-player setting rather than relying on the
 * server's game rule. Vanilla uses it to decide whether the fresh player instance
 * inherits the old instance's inventory, XP and score. The setting must be read
 * from the OLD player (the method parameter) because attachments have not been
 * copied onto the new instance ({@code this}) yet at this point.
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerRestoreMixin {

    @Redirect(
            method = "restoreFrom",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"
            )
    )
    private Object redirectKeepInventory(GameRules gameRules, GameRule<Boolean> rule, ServerPlayer oldPlayer, boolean keepEverything) {
        var serverValue = gameRules.get(rule);
        return PlayerKeepInventoryData.INSTANCE.getEffectiveKeepInventory(oldPlayer, serverValue);
    }
}
