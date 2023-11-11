package work.lclpnet.combatctl.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.impl.CombatConfig;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Shadow protected ItemStack activeItemStack;

    @SuppressWarnings("ConstantValue")
    @Inject(
            method = "consumeItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;finishUsing(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/item/ItemStack;",
                    shift = At.Shift.AFTER
            )
    )
    protected void combatControl$completeUsingItem(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(player);

        if (config.isModernNotchApple() || !activeItemStack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) return;

        player.removeStatusEffect(StatusEffects.ABSORPTION);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 600, 4));
    }
}
