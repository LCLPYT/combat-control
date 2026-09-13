package work.lclpnet.combatctl.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;
import work.lclpnet.combatctl.mixin.client.ItemStackRenderState$LayerRenderStateAccessor;
import work.lclpnet.combatctl.mixin.client.ItemStackRenderStateAccessor;
import work.lclpnet.combatctl.type.ToolInfo;

/**
 * Recreates the third person sword blocking item transform of Minecraft 1.7.10.
 */
public class ClassicBlockingRenderer {

    private ClassicBlockingRenderer() {}

    /**
     * Whether the entity is blocking with a sword in the arm.
     *
     * @param state The render state of the entity.
     * @param arm The arm to check.
     * @return Whether the classic blocking animation can be used for that arm.
     */
    public static boolean shouldApply(ArmedEntityRenderState state, HumanoidArm arm) {
        var pose = arm == HumanoidArm.RIGHT ? state.rightArmPose : state.leftArmPose;

        if (pose != HumanoidModel.ArmPose.BLOCK) return true;

        return ToolInfo.of(state.getUseItemStackForArm(arm))
                .filter(ToolInfo::isSword)
                .isEmpty();
    }

    /**
     * Applies the 1.7.10 item transform to the pose stack, relative to the arm the item is held in.
     * The transform of the current item model is undone, because it is applied again when the item is submitted.
     * <p>
     * Values taken from 1.7.10, referring to these classes (legacy yarn mappings build 551):
     * - PlayerEntityRenderer
     * - HeldItemRenderer
     * <p>
     * The rest of the inverse transform code was ported from <a href="https://github.com/Fuzss/sword-blocking-mechanics/blob/e22f744511da3e393e70af0ba1145e0473358db2/Common/src/main/java/fuzs/swordblockingmechanics/common/client/helper/AdvancedBlockingRenderer.java">sword-blocking-mechanics by Fuzss</a>, licensed MPLv2.
     *
     * @param poseStack The pose stack, already translated to the hand.
     * @param item The item render state that will be submitted afterward.
     * @param arm The arm holding the item.
     */
    public static void applyItemTransform(PoseStack poseStack, ItemStackRenderState item, HumanoidArm arm) {
        boolean leftHand = arm == HumanoidArm.LEFT;

        poseStack.translate((leftHand ? 1f : -1f) / 16f, 0.4375f, 0.0625f);

        poseStack.translate(leftHand ? -0.035f : 0.05f, leftHand ? 0.045f : 0f, leftHand ? -0.135f : -0.1f);
        poseStack.mulPose(Axis.YP.rotationDegrees((leftHand ? -1f : 1f) * -50f));
        poseStack.mulPose(Axis.XP.rotationDegrees(-10f));
        poseStack.mulPose(Axis.ZP.rotationDegrees((leftHand ? -1f : 1f) * -60f));

        poseStack.translate(0f, 0.1875f, 0f);
        // 1.7.10 scaled the y-axis negatively, which is unsupported since 1.16, hence the flipped rotations
        poseStack.scale(0.625f, 0.625f, 0.625f);
        poseStack.mulPose(Axis.XP.rotationDegrees(180f));
        poseStack.mulPose(Axis.XN.rotationDegrees(-100f));
        poseStack.mulPose(Axis.YN.rotationDegrees(leftHand ? 35f : 45f));

        poseStack.translate(0f, -0.3f, 0f);
        poseStack.scale(1.5f, 1.5f, 1.5f);
        poseStack.mulPose(Axis.YN.rotationDegrees(50f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(335f));
        poseStack.translate(-0.9375f, -0.0625f, 0f);

        // move the modern centered item model onto the origin of the old flat item quad
        poseStack.translate(0.5f, 0.5f, 0.25f);
        poseStack.mulPose(Axis.YN.rotationDegrees(180f));
        poseStack.translate(0f, 0f, 0.28125f);

        var layer = ((ItemStackRenderStateAccessor) item).combatControl$firstLayer();
        var transform = ((ItemStackRenderState$LayerRenderStateAccessor) layer).combatControl$getItemTransform();

        undoItemTransform(poseStack, transform, leftHand);
    }

    /**
     * Inverse of {@link ItemTransform#apply(boolean, PoseStack.Pose)}, except for its final centering translation,
     * which is compensated for by the transform above.
     * <p>
     * Inverse transform approach ported from <a href="https://github.com/Fuzss/sword-blocking-mechanics/blob/e22f744511da3e393e70af0ba1145e0473358db2/Common/src/main/java/fuzs/swordblockingmechanics/common/client/helper/AdvancedBlockingRenderer.java">sword-blocking-mechanics by Fuzss</a>, licensed MPLv2.
     */
    private static void undoItemTransform(PoseStack poseStack, ItemTransform transform, boolean leftHand) {
        if (transform == ItemTransform.NO_TRANSFORM) return;

        float angleX = transform.rotation().x();
        float angleY = leftHand ? -transform.rotation().y() : transform.rotation().y();
        float angleZ = leftHand ? -transform.rotation().z() : transform.rotation().z();

        var rotation = new Quaternionf()
                .rotationXYZ(angleX * Mth.DEG_TO_RAD, angleY * Mth.DEG_TO_RAD, angleZ * Mth.DEG_TO_RAD)
                .conjugate();

        poseStack.scale(1f / transform.scale().x(), 1f / transform.scale().y(), 1f / transform.scale().z());
        poseStack.mulPose(rotation);
        poseStack.translate((leftHand ? -1f : 1f) * -transform.translation().x(),
                -transform.translation().y(), -transform.translation().z());
    }
}
