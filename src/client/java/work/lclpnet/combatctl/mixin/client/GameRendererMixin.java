package work.lclpnet.combatctl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControlClient;
import work.lclpnet.combatctl.type.CombatControlClientPlayer;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow @Final private Minecraft minecraft;

    @Inject(
            method = "bobView",
            at = @At("TAIL")
    )
    public void combatControl$bobView(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        var control = CombatControlClient.get();
        var config = control.config();

        if (!config.isOldBobbing() || (config.isServerBobbingOverride() && control.abilities().disableOldBobbing)) return;

        if (this.minecraft.getCameraEntity() instanceof AbstractClientPlayer player) {
            var cccPlayer = (CombatControlClientPlayer) player;
            float tickDelta = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            float rot = Mth.lerp(tickDelta, cccPlayer.combatControl$getPrevCameraPitch(), cccPlayer.combatControl$getCameraPitch());
            poseStack.mulPose(Axis.XP.rotationDegrees(rot));
        }
    }
}
