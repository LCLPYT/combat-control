package work.lclpnet.combatctl.mixin.client;

import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HeldItemRenderer.class)
public interface HeldItemRendererAccessor {

    @Invoker("applyEatOrDrinkTransformation")
    void combatControl$callApplyEatOrDrinkTransformation(MatrixStack matrixStack, float partialTicks, Arm handIn, ItemStack stack);

    @Invoker("applySwingOffset")
    void combatControl$callApplySwingOffset(MatrixStack matrixStackIn, Arm handIn, float swingProgress);

    @Invoker("applyEquipOffset")
    void combatControl$callApplyEquipOffset(MatrixStack matrixStackIn, Arm handIn, float equippedProgress);
}
