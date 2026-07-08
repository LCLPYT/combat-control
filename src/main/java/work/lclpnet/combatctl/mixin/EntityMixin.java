package work.lclpnet.combatctl.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.impl.StaticCombatControl;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(
            method = "getPickRadius",
            at = @At("HEAD"),
            cancellable = true
    )
    public void combatControl$modifyTargetingMargin(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity) (Object) this;

        // don't grow hitboxes of item frames, paintings etc.
        if (self instanceof BlockAttachedEntity) return;

        if (!StaticCombatControl.get().getContext().globalConfig().isLargerHitboxes()) return;

        cir.setReturnValue(0.1f);
    }
}
