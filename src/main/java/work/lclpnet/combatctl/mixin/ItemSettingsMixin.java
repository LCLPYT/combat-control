package work.lclpnet.combatctl.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.type.ToolInfo;
import work.lclpnet.combatctl.type.ToolInfoCapture;
import work.lclpnet.combatctl.type.ToolType;

@Mixin(Item.Properties.class)
public class ItemSettingsMixin implements ToolInfoCapture {

    @Unique @Nullable private ToolInfo toolInfo = null;

    @Override
    public @Nullable ToolInfo combatControl$getToolInfo() {
        return toolInfo;
    }

    @Inject(
            method = "sword",
            at = @At("HEAD")
    )
    public void combatControl$captureSwordInfo(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline, CallbackInfoReturnable<Item.Properties> cir) {
        toolInfo = new ToolInfo(ToolType.SWORD, material);
    }

    @Inject(
            method = "pickaxe",
            at = @At("HEAD")
    )
    public void combatControl$capturePickaxeInfo(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline, CallbackInfoReturnable<Item.Properties> cir) {
        toolInfo = new ToolInfo(ToolType.PICKAXE, material);
    }

    @Inject(
            method = "axe",
            at = @At("HEAD")
    )
    public void combatControl$captureAxeInfo(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline, CallbackInfoReturnable<Item.Properties> cir) {
        toolInfo = new ToolInfo(ToolType.AXE, material);
    }

    @Inject(
            method = "hoe",
            at = @At("HEAD")
    )
    public void combatControl$captureHoeInfo(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline, CallbackInfoReturnable<Item.Properties> cir) {
        toolInfo = new ToolInfo(ToolType.HOE, material);
    }

    @Inject(
            method = "shovel",
            at = @At("HEAD")
    )
    public void combatControl$captureShovelInfo(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline, CallbackInfoReturnable<Item.Properties> cir) {
        toolInfo = new ToolInfo(ToolType.SHOVEL, material);
    }
}
