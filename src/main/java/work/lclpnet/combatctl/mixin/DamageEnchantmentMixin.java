package work.lclpnet.combatctl.mixin;

import net.minecraft.enchantment.DamageEnchantment;
import net.minecraft.entity.EntityGroup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.GlobalCombatControl;

@Mixin(DamageEnchantment.class)
public class DamageEnchantmentMixin {

    @Shadow @Final public int typeIndex;

    @Inject(
            method = "getAttackDamage",
            at = @At("HEAD"),
            cancellable = true
    )
    public void combatControl$modifyAttackDamage(int level, EntityGroup group, CallbackInfoReturnable<Float> cir) {
        if (GlobalCombatControl.get().getGlobalConfig().isModernSharpness()) return;

        if (this.typeIndex == 0) cir.setReturnValue(level * 1.25f);
    }
}
