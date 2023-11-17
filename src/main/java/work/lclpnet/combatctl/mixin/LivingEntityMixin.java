package work.lclpnet.combatctl.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.impl.CombatConfig;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    protected ItemStack activeItemStack;

    @Unique
    protected double knockBackStrength = Double.NaN;

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

    @SuppressWarnings("ConstantValue")
    @Inject(
            method = "takeKnockback",
            at = @At("HEAD")
    )
    public void combatControl$beforeKnockBack(double strength, double ratioX, double ratioZ, CallbackInfo callback) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(player);

        if (!config.isStrongKnockBackInAir()) return;

        // knock back functionality from GoldenAgeCombat, but only players are affected
        if (player.isOnGround() && !player.isTouchingWater()) {
            knockBackStrength = strength * (1.0 - player.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE));

            final Vec3d deltaMovement = player.getVelocity();
            player.setVelocity(deltaMovement.x, Math.min(0.4, deltaMovement.y / 2.0D + strength), deltaMovement.x);
        }
    }

    @ModifyVariable(
            method = "takeKnockback",
            at = @At(
                    value = "HEAD"
            ),
            ordinal = 0,
            argsOnly = true
    )
    public double combatControl$modifyKnockBackStrength(double strength) {
        if (Double.isNaN(knockBackStrength)) return strength;

        strength = knockBackStrength;
        knockBackStrength = Double.NaN;

        return strength;
    }
}
