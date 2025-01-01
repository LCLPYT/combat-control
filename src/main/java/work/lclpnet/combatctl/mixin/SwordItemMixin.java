package work.lclpnet.combatctl.mixin;

import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.type.ToolMaterialCapture;

@Mixin(SwordItem.class)
public class SwordItemMixin implements ToolMaterialCapture {

    @Unique private ToolMaterial toolMaterial = null;

    @Override
    public ToolMaterial combatControl$getToolMaterial() {
        return toolMaterial;
    }

    @Override
    public void combatControl$setToolMaterial(ToolMaterial toolMaterial) {
        this.toolMaterial = toolMaterial;
    }

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void combatControl$captureToolMaterial(ToolMaterial material, float attackDamage, float attackSpeed, Item.Settings settings, CallbackInfo ci) {
        this.toolMaterial = material;
    }
}
