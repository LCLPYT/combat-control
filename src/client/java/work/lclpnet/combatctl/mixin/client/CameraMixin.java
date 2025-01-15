package work.lclpnet.combatctl.mixin.client;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControlClient;

@Mixin(Camera.class)
public class CameraMixin {

    @Shadow private Entity focusedEntity;
    @Shadow private float lastCameraY;
    @Shadow private float cameraY;

    // combatControl$updateEyeHeight is taken from GoldenAgeCombat
    @Inject(
            method = "updateEyeHeight",
            at = @At("TAIL")
    )
    private void combatControl$updateEyeHeight(CallbackInfo ci) {
        if (!CombatControlClient.get().config().isInstantEyeHeight() || focusedEntity == null) return;

        this.lastCameraY = this.cameraY = focusedEntity.getStandingEyeHeight();
    }
}
