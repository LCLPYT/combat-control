package work.lclpnet.combatctl.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControlClient;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow protected abstract void applyItemArmAttackTransform(PoseStack matrices, HumanoidArm arm, float swingProgress);

    @Shadow @Final private Minecraft minecraft;

    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
                    shift = At.Shift.AFTER
            )
    )
    public void combatControl$onRenderFirstPersonItem(AbstractClientPlayer player, float tickProgress, float pitch, InteractionHand hand, float swingProgress, ItemStack stack, float equipProgress, PoseStack matrices, SubmitNodeCollector orderedRenderCommandQueue, int light, CallbackInfo ci) {
        if (!CombatControlClient.get().abilities().renderArmSwingWhileUsing
                || stack.isEmpty() || stack.is(Items.FILLED_MAP) || !player.isUsingItem()
                || player.getUseItemRemainingTicks() <= 0 || player.getUsedItemHand() != hand)
            return;

        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        applyItemArmAttackTransform(matrices, arm, swingProgress);
    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"
            )
    )
    public float combatControl$removeCooldownEquipAnimation(LocalPlayer instance, float v, Operation<Float> original) {
        if (CombatControlClient.get().abilities().attackCooldown) {
            return original.call(instance, v);
        }

        return 1;
    }

    @Inject(
            method = "itemUsed",
            at = @At("HEAD"),
            cancellable = true
    )
    public void combatControl$onResetEquipProgress(InteractionHand hand, CallbackInfo ci) {
        if (CombatControlClient.get().abilities().noReequipWhenUsing
                && minecraft.player != null && minecraft.player.isUsingItem()
                && minecraft.player.getUsedItemHand() == hand) {
            ci.cancel();
        }
    }
}
