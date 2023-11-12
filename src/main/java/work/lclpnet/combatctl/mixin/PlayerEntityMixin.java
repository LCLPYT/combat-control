package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.impl.CombatConfig;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @SuppressWarnings("ConstantValue")
    @Inject(
            method = "getAttackCooldownProgress",
            at = @At("HEAD"),
            cancellable = true
    )
    public void combatControl$getAttackCooldownProgress(float baseTime, CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(player);

        if (config.isAttackCooldown()) return;

        cir.setReturnValue(1.0F);
    }

    @SuppressWarnings("ConstantValue")
    @WrapWithCondition(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"
            )
    )
    public boolean combatControl$playCombatSoundsIfEnabled(World world, @Nullable PlayerEntity except, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return true;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(player);

        return config.isModernHitSounds() || (config.isSweepAttack() && sound == SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP);
    }

    @SuppressWarnings("ConstantValue")
    @WrapWithCondition(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerWorld;spawnParticles(Lnet/minecraft/particle/ParticleEffect;DDDIDDDD)I"
            )
    )
    public boolean combatControl$spawnCombatParticlesIfEnabled(ServerWorld world, ParticleEffect particle, double x, double y, double z, int count, double deltaX, double deltaY, double deltaZ, double speed) {
        if (particle != ParticleTypes.DAMAGE_INDICATOR || !((Object) this instanceof ServerPlayerEntity player)) return true;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(player);

        return config.isModernHitParticle();
    }

    @SuppressWarnings("ConstantValue")
    @Inject(
            method = "spawnSweepAttackParticles",
            at = @At("HEAD"),
            cancellable = true
    )
    public void combatControl$spawnSweepAttackParticles(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(player);

        if (!config.isModernHitParticle() && !config.isSweepAttack()) {
            ci.cancel();
        }
    }

    @SuppressWarnings("ConstantValue")
    @ModifyVariable(method = "attack", at = @At("LOAD"), ordinal = 3, slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;spawnSweepAttackParticles()V")))
    public boolean combatControl$modifySweepAttack(boolean original, Entity target) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return original;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(player);

        if (config.isSweepAttack()) return original;

        // only trigger sweeping edge when the player has the sweeping edge enchantment on their weapon
        return original && EnchantmentHelper.getSweepingMultiplier(player) > 0.0F;
    }

    @SuppressWarnings("ConstantValue")
    @Inject(
            method = "damage",
            at = @At(
                    value = "RETURN",
                    ordinal = 0
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/entity/damage/DamageSource;isScaledWithDifficulty()Z"
                    )
            ),
            cancellable = true
    )
    public void combatControl$onWeakDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(player);

        if (config.isPreventWeakAttackKnockBack()) return;

        if (amount == 0.0F && getWorld().getDifficulty() != Difficulty.PEACEFUL) {
            callback.setReturnValue(super.damage(source, amount));
        }
    }
}
