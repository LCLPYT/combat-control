package work.lclpnet.combatctl.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import work.lclpnet.combatctl.api.CombatControlClient;
import work.lclpnet.combatctl.mixin.client.HeldItemRendererAccessor;

/**
 * @implNote This implementation is taken from GoldenAgeCombat and remapped into yarn mappings.
 */
public class ItemInHandHandler {
    
    public static boolean onRenderHand(PlayerEntity player, Hand hand, ItemStack stack, MatrixStack poseStack, VertexConsumerProvider multiBufferSource, int packedLight, float partialTick, float interpolatedPitch, float swingProgress, float equipProgress) {
        if (!CombatControlClient.get().getAbilities().attackWhileUsing || stack.isEmpty() || stack.isOf(Items.FILLED_MAP)) return false;

        if (!player.isUsingItem() || player.getItemUseTimeLeft() <= 0 || player.getActiveHand() != hand)
            return false;

        MinecraftClient minecraft = MinecraftClient.getInstance();
        HeldItemRenderer itemRenderer = minecraft.gameRenderer.firstPersonRenderer;
        boolean mainHand = hand == Hand.MAIN_HAND;
        Arm humanoidArm = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean rightArm = (mainHand ? player.getMainArm() : player.getMainArm().getOpposite()) == Arm.RIGHT;
        poseStack.push();
        // all this does is call ItemInHandRenderer::applyItemArmTransform and ItemInHandRenderer::applyItemArmAttackTransform for all use animations,
        // to allow the main animation to still respect arm swings / attacks and not abruptly cancel them out
        switch (stack.getUseAction()) {
            case NONE, BLOCK -> {
                ((HeldItemRendererAccessor) itemRenderer).combatControl$callApplyEquipOffset(poseStack, humanoidArm, equipProgress);
                ((HeldItemRendererAccessor) itemRenderer).combatControl$callApplySwingOffset(poseStack, humanoidArm, swingProgress);
            }
            case EAT, DRINK -> {
                ((HeldItemRendererAccessor) itemRenderer).combatControl$callApplyEatOrDrinkTransformation(poseStack, partialTick, humanoidArm, stack);
                ((HeldItemRendererAccessor) itemRenderer).combatControl$callApplyEquipOffset(poseStack, humanoidArm, equipProgress);
                ((HeldItemRendererAccessor) itemRenderer).combatControl$callApplySwingOffset(poseStack, humanoidArm, swingProgress);
            }
            case BOW -> {
                ((HeldItemRendererAccessor) itemRenderer).combatControl$callApplyEquipOffset(poseStack, humanoidArm, equipProgress);
                ((HeldItemRendererAccessor) itemRenderer).combatControl$callApplySwingOffset(poseStack, humanoidArm, swingProgress);
                applyBowTransform(poseStack, partialTick, humanoidArm, stack, player);
            }
            case SPEAR -> {
                ((HeldItemRendererAccessor) itemRenderer).combatControl$callApplyEquipOffset(poseStack, humanoidArm, equipProgress);
                ((HeldItemRendererAccessor) itemRenderer).combatControl$callApplySwingOffset(poseStack, humanoidArm, swingProgress);
                applyTridentTransform(poseStack, partialTick, humanoidArm, stack, player);
            }
            case CROSSBOW -> {
                ((HeldItemRendererAccessor) itemRenderer).combatControl$callApplyEquipOffset(poseStack, humanoidArm, equipProgress);
                ((HeldItemRendererAccessor) itemRenderer).combatControl$callApplySwingOffset(poseStack, humanoidArm, swingProgress);
                applyCrossbowTransform(poseStack, partialTick, humanoidArm, stack, player);
            }
        }
        itemRenderer.renderItem(player, stack, rightArm ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !rightArm, poseStack, multiBufferSource, packedLight);
        poseStack.pop();
        return true;

    }

    private static void applyBowTransform(MatrixStack poseStack, float partialTick, Arm humanoidArm, ItemStack stack, PlayerEntity player) {
        int direction = humanoidArm == Arm.RIGHT ? 1 : -1;
        poseStack.translate(direction * -0.2785682F, 0.18344387F, 0.15731531F);
        poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-13.935F));
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * 35.3F));
        poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(direction * -9.785F));
        float f8 = stack.getMaxUseTime() - (player.getItemUseTimeLeft() - partialTick + 1.0F);
        float f12 = f8 / 20.0F;
        f12 = (f12 * f12 + f12 * 2.0F) / 3.0F;
        if (f12 > 1.0F) {
            f12 = 1.0F;
        }
        if (f12 > 0.1F) {
            float f15 = MathHelper.sin((f8 - 0.1F) * 1.3F);
            float f18 = f12 - 0.1F;
            float f20 = f15 * f18;
            poseStack.translate(f20 * 0.0F, f20 * 0.004F, f20 * 0.0F);
        }
        poseStack.translate(f12 * 0.0F, f12 * 0.0F, f12 * 0.04F);
        poseStack.scale(1.0F, 1.0F, 1.0F + f12 * 0.2F);
        poseStack.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(direction * 45.0F));
    }

    private static void applyTridentTransform(MatrixStack poseStack, float partialTick, Arm humanoidArm, ItemStack stack, PlayerEntity player) {
        int direction = humanoidArm == Arm.RIGHT ? 1 : -1;
        poseStack.translate(direction * -0.5F, 0.7F, 0.1F);
        poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-55.0F));
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * 35.3F));
        poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(direction * -9.785F));
        float f7 = stack.getMaxUseTime() - (player.getItemUseTimeLeft() - partialTick + 1.0F);
        float f11 = f7 / 10.0F;
        if (f11 > 1.0F) {
            f11 = 1.0F;
        }
        if (f11 > 0.1F) {
            float f14 = MathHelper.sin((f7 - 0.1F) * 1.3F);
            float f17 = f11 - 0.1F;
            float f19 = f14 * f17;
            poseStack.translate(f19 * 0.0F, f19 * 0.004F, f19 * 0.0F);
        }
        poseStack.translate(0.0D, 0.0D, f11 * 0.2F);
        poseStack.scale(1.0F, 1.0F, 1.0F + f11 * 0.2F);
        poseStack.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(direction * 45.0F));
    }

    private static void applyCrossbowTransform(MatrixStack poseStack, float partialTick, Arm humanoidArm, ItemStack stack, PlayerEntity player) {
        int direction = humanoidArm == Arm.RIGHT ? 1 : -1;
        poseStack.translate(direction * -0.4785682F, -0.094387F, 0.05731531F);
        poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * 65.3F));
        poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(direction * -9.785F));
        float f9 = stack.getMaxUseTime() - (player.getItemUseTimeLeft() - partialTick + 1.0F);
        float f13 = f9 / CrossbowItem.getPullTime(stack);
        if (f13 > 1.0F) {
            f13 = 1.0F;
        }
        if (f13 > 0.1F) {
            float f16 = MathHelper.sin((f9 - 0.1F) * 1.3F);
            float f3 = f13 - 0.1F;
            float f4 = f16 * f3;
            poseStack.translate(f4 * 0.0F, f4 * 0.004F, f4 * 0.0F);
        }
        poseStack.translate(f13 * 0.0F, f13 * 0.0F, f13 * 0.04F);
        poseStack.scale(1.0F, 1.0F, 1.0F + f13 * 0.2F);
        poseStack.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(direction * 45.0F));
    }
}
