package work.lclpnet.combatctl.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControl;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(
            method = "getTargetingMargin",
            at = @At("HEAD"),
            cancellable = true
    )
    public void combatControl$modifyTargetingMargin(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity) (Object) this;

        if (!CombatControl.get(self.getServer()).globalConfig().isLargerHitboxes()) return;

        cir.setReturnValue(0.1f);
    }
}
