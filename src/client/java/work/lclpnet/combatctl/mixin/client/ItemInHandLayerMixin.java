package work.lclpnet.combatctl.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControlClient;
import work.lclpnet.combatctl.impl.ClassicBlockingRenderer;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin<S extends ArmedEntityRenderState, M extends EntityModel<S> & ArmedModel<S>> extends RenderLayer<S, @NonNull M> {

    public ItemInHandLayerMixin(RenderLayerParent<S, @NonNull M> renderer) {
        super(renderer);
    }

    @Inject(
            method = "submitArmWithItem",
            at = @At("HEAD"),
            cancellable = true
    )
    protected void combatControl$classicBlockingItem(S state, ItemStackRenderState item, ItemStack itemStack, HumanoidArm arm,
                                                     PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                                                     CallbackInfo ci) {

        if (item.isEmpty() || !CombatControlClient.get().config().isClassicBlockAnimation()
            || ClassicBlockingRenderer.shouldApply(state, arm)) return;

        // reference: PlayerEntityRenderer (legacy yarn mappings build 551)
        poseStack.pushPose();

        getParentModel().translateToHand(state, arm, poseStack);
        ClassicBlockingRenderer.applyItemTransform(poseStack, item, arm);

        item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);

        poseStack.popPose();

        ci.cancel();
    }
}
