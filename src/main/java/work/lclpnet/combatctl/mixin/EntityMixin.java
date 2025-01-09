package work.lclpnet.combatctl.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.impl.StaticCombatControl;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(
            method = "getTargetingMargin",
            at = @At("HEAD"),
            cancellable = true
    )
    public void combatControl$modifyTargetingMargin(CallbackInfoReturnable<Float> cir) {
        if (!StaticCombatControl.get().globalConfig().isLargerHitboxes()) return;

        cir.setReturnValue(0.1f);
    }
}
