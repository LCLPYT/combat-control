package work.lclpnet.combatctl.mixin.client;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.type.CombatControlClientPlayer;

@Mixin(Player.class)
public class PlayerMixin implements CombatControlClientPlayer {

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
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getHealth()F"
            )
    )
    private void combatControl$updateCameraPitch(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        float adjustmentAngle = (float) Math.atan(-self.getDeltaMovement().y() * 0.2F) * 15.0F;

        if (self.onGround() || self.getHealth() <= 0.0F) {
            adjustmentAngle = 0.0F;
        }

        prevCameraPitch = cameraPitch;
        cameraPitch = cameraPitch + (adjustmentAngle - cameraPitch) * 0.8F;
    }
}
