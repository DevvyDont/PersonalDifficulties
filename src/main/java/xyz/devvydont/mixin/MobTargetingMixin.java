package xyz.devvydont.mixin;

import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.devvydont.data.PlayerDifficultyData;

/**
 * Injects an additional guard clause into valid mob targeting selection.
 * If a player is set to peaceful mode, perform same logic as if they were in creative
 * mode where they are not considered a valid target.
 */
@Mixin(Mob.class)
public class MobTargetingMixin {

    @Inject(
            method = "asValidTarget",
            at = @At("HEAD"),
            cancellable = true
    )
    private void beforeValidTarget(LivingEntity target, CallbackInfoReturnable<LivingEntity> cir) {
        if (target instanceof Player player)
            if (PlayerDifficultyData.INSTANCE.getPlayerDifficulty(player) == Difficulty.PEACEFUL)
                cir.setReturnValue(null);
    }

}
