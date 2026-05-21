package xyz.devvydont.mixin;

import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.devvydont.data.PlayerDifficultyData;

/**
 * Injects before determining if this zombie can break doors. If the current
 * target is a player that is NOT on hard, then we for sure cannot allow
 * the zombie to break a door.
 */
@Mixin(Zombie.class)
public class ZombieDoorBreakingMixin {

    @Inject(
            method = "canBreakDoors",
            at = @At("HEAD"),
            cancellable = true
    )
    private void beforeCanBreakDoors(CallbackInfoReturnable<Boolean> cir) {
        var zombie = (Zombie) (Object) this;
        if (zombie.getTarget() instanceof Player player)
            if (PlayerDifficultyData.INSTANCE.getPlayerDifficulty(player) != Difficulty.HARD)
                cir.setReturnValue(false);
    }

}
