package xyz.devvydont.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.devvydont.data.PlayerDifficultyData;

@Mixin(Player.class)
public class ServerPlayerDamageMixin {

    @Redirect(
            method = "hurtServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getDifficulty()Lnet/minecraft/world/Difficulty;"
            )
    )
    private Difficulty redirectDifficulty(ServerLevel level) {
        var player = (Player) (Object) this;
        return PlayerDifficultyData.INSTANCE.getPlayerDifficulty(player);
    }
}
