package work.lclpnet.combatctl.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControlClient;

@Mixin(Camera.class)
public class CameraMixin {

    @Shadow private Entity entity;
    @Shadow private float eyeHeightOld;
    @Shadow private float eyeHeight;

    // combatControl$updateEyeHeight is taken from GoldenAgeCombat
    @Inject(
            method = "tick",
            at = @At("TAIL")
    )
    private void combatControl$updateEyeHeight(CallbackInfo ci) {
        if (!CombatControlClient.get().config().isInstantEyeHeight() || entity == null) return;

        this.eyeHeightOld = this.eyeHeight = entity.getEyeHeight();
    }
}
