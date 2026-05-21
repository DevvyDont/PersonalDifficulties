package xyz.devvydont.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.devvydont.data.PlayerDifficultyData;

/**
 * This mixin hijacks into food exhaustion logic to override where the
 * computed difficulty is to use our mod's set difficulty rather than relying on
 * the server's difficulty. This causes hunger to decay at different rates.
 */
@Mixin(FoodData.class)
public class FoodDataExhaustionMixin {

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getDifficulty()Lnet/minecraft/world/Difficulty;"
            )
    )
    private Difficulty redirectDifficulty(ServerLevel level, final ServerPlayer player) {
        return PlayerDifficultyData.INSTANCE.getPlayerDifficulty(player);
    }

}
