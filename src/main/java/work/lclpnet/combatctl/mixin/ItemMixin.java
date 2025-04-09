package work.lclpnet.combatctl.mixin;

import net.minecraft.item.Item;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.type.ToolInfo;
import work.lclpnet.combatctl.type.ToolInfoCapture;

@Mixin(Item.class)
public class ItemMixin implements ToolInfoCapture {

    @Unique @Nullable private ToolInfo toolInfo = null;

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    public void combatControl$initToolInfo(Item.Settings settings, CallbackInfo ci) {
        toolInfo = ((ToolInfoCapture) settings).combatControl$getToolInfo();
    }

    @Override
    public @Nullable ToolInfo combatControl$getToolInfo() {
        return toolInfo;
    }
}
