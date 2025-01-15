package work.lclpnet.combatctl.mixin.client;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.type.CombatControlClientPlayer;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements CombatControlClientPlayer {

    @Unique private float cameraPitch, prevCameraPitch;

    @Override
    public float combatControl$getCameraPitch() {
        return cameraPitch;
    }

    @Override
    public float combatControl$getPrevCameraPitch() {
        return prevCameraPitch;
    }

    @Inject(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;getHealth()F"
            )
    )
    private void combatControl$updateCameraPitch(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        float adjustmentAngle = (float) Math.atan(-self.getVelocity().getY() * 0.2F) * 15.0F;

        if (self.isOnGround() || self.getHealth() <= 0.0F) {
            adjustmentAngle = 0.0F;
        }

        prevCameraPitch = cameraPitch;
        cameraPitch = cameraPitch + (adjustmentAngle - cameraPitch) * 0.8F;
    }
}
