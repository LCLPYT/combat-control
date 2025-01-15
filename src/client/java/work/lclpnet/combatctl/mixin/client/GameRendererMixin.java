package work.lclpnet.combatctl.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
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

    @Shadow @Final private MinecraftClient client;

    @Inject(
            method = "bobView",
            at = @At("TAIL")
    )
    public void combatControl$bobView(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (!CombatControlClient.get().config().isOldBobbing()) return;

        if (this.client.getCameraEntity() instanceof AbstractClientPlayerEntity player) {
            var cccPlayer = (CombatControlClientPlayer) player;
            float rot = MathHelper.lerp(tickDelta, cccPlayer.combatControl$getPrevCameraPitch(), cccPlayer.combatControl$getCameraPitch());
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rot));
        }
    }
}
