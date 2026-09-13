package work.lclpnet.combatctl.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControlClient;
import work.lclpnet.combatctl.impl.ClassicBlockingRenderer;

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin {

    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart head;

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/HumanoidModel;setupAttackAnimation(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V"
            )
    )
    public void combatControl$classicBlockingPose(HumanoidRenderState state, CallbackInfo ci) {
        if (!state.isUsingItem || !CombatControlClient.get().config().isClassicBlockAnimation()) return;

        boolean rightHanded = state.mainArm == HumanoidArm.RIGHT;
        boolean mainHandUsed = state.useItemHand == InteractionHand.MAIN_HAND;
        HumanoidArm arm = mainHandUsed == rightHanded ? HumanoidArm.RIGHT : HumanoidArm.LEFT;

        if (ClassicBlockingRenderer.shouldApply(state, arm)) return;

        ModelPart part = arm == HumanoidArm.RIGHT ? rightArm : leftArm;

        // undo the head coupling and the sideways rotation that were added in 1.8, leaving the 1.7.10 pitch
        // reference: PlayerEntityRenderer, BipedModel, HumanoidModel (legacy yarn mappings build 551)
        part.xRot -= Mth.clamp(head.xRot, (float) (-Math.PI * 4.0 / 9.0), 0.43633232f);
        part.yRot = 0f;
    }
}
