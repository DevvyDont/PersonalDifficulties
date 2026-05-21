package xyz.devvydont.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.devvydont.data.PlayerDifficultyData;

/**
 * This mixin hijacks into regeneration logic to override where the
 * computed difficulty is to use our mod's set difficulty rather than relying on
 * the server's difficulty. This provides regeneration behavior when on peaceful mode.
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerRegenerationMixin {

    @Redirect(
            method = "tickRegeneration",
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
