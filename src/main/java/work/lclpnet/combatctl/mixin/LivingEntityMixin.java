package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.api.KnockbackVariant;
import work.lclpnet.combatctl.config.PlayerConfig;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    protected ItemStack activeItemStack;

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

        PlayerConfig config = CombatControl.get(player.getServer()).playerConfig(player);

        if (config.isModernNotchApple() || !activeItemStack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) return;

        player.removeStatusEffect(StatusEffects.ABSORPTION);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 600, 4));
    }

    @WrapOperation(
            method = "takeKnockback",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;setVelocity(DDD)V"
            )
    )
    private void combatControl$modifyVelocity(LivingEntity instance, double x, double y, double z, Operation<Void> original,
                                              @Local(ordinal = 0) Vec3d velocity, @Local(ordinal = 1) Vec3d knockbackDir,
                                              @Local(ordinal = 0, argsOnly = true) double strength) {
        if (!(instance instanceof ServerPlayerEntity player)) {
            original.call(instance, x, y, z);
            return;
        }

        PlayerConfig config = CombatControl.get(player.getServer()).playerConfig(player);

        if (config.getKnockbackVariant() == KnockbackVariant.NO_SCALING) {
            if (!player.isTouchingWater()) {
                player.setVelocity(velocity.x / 2.0 - knockbackDir.x, Math.min(0.4, velocity.y / 2.0 + strength), velocity.z / 2.0 - knockbackDir.z);
                return;
            }
        }

        original.call(instance, x, y, z);
    }
}
