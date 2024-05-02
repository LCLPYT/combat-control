package work.lclpnet.combatctl.mixin;

import net.minecraft.enchantment.DamageEnchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.GlobalCombatControl;

@Mixin(DamageEnchantment.class)
public class DamageEnchantmentMixin {

    @Inject(
            method = "getAttackDamage",
            at = @At("HEAD"),
            cancellable = true
    )
    public void combatControl$modifyAttackDamage(int level, @Nullable EntityType<?> entityType, CallbackInfoReturnable<Float> cir) {
        if (GlobalCombatControl.get().getGlobalConfig().isModernSharpness()) return;

        DamageEnchantment self = (DamageEnchantment) (Object) this;

        if (Enchantments.SHARPNESS == self) {
            cir.setReturnValue(level * 1.25f);
        }
    }
}
