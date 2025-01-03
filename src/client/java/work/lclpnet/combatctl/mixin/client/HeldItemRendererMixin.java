package work.lclpnet.combatctl.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControlClient;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Shadow protected abstract void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress);

    @Shadow @Final private MinecraftClient client;

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V",
                    shift = At.Shift.AFTER
            )
    )
    public void combatControl$onRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!CombatControlClient.get().getAbilities().renderArmSwingWhileUsing
                || stack.isEmpty() || stack.isOf(Items.FILLED_MAP) || !player.isUsingItem()
                || player.getItemUseTimeLeft() <= 0 || player.getActiveHand() != hand)
            return;

        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        applySwingOffset(matrices, arm, swingProgress);
    }

    @WrapOperation(
            method = "updateHeldItems",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;getAttackCooldownProgress(F)F"
            )
    )
    public float combatControl$removeCooldownEquipAnimation(ClientPlayerEntity instance, float v, Operation<Float> original) {
        if (CombatControlClient.get().getAbilities().attackCooldown) {
            return original.call(instance, v);
        }

        return 1;
    }

    @Inject(
            method = "resetEquipProgress",
            at = @At("HEAD"),
            cancellable = true
    )
    public void combatControl$onResetEquipProgress(Hand hand, CallbackInfo ci) {
        if (CombatControlClient.get().getAbilities().noReequipWhenUsing
                && client.player != null && client.player.isUsingItem()
                && client.player.getActiveHand() == hand) {
            ci.cancel();
        }
    }
}
