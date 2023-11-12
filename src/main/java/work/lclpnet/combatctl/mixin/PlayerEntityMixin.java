package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
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
import org.spongepowered.asm.mixin.Unique;
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

    @Unique
    private boolean sprintDuringAttack;

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

        // check if weak attacks are enabled or if fishing rod knock back is enabled
        if (config.isPreventWeakAttackKnockBack()
            && (config.isPreventFishingRodKnockBack() || !(source.getSource() instanceof FishingBobberEntity))) return;

        if (Math.abs(amount) < 1e-9f && getWorld().getDifficulty() != Difficulty.PEACEFUL) {
            callback.setReturnValue(super.damage(source, amount));
        }
    }

    // combatControl$initialAttackSprintState is taken from GoldenAgeCombat
    @Inject(method = "attack", at = @At("HEAD"))
    public void combatControl$initialAttackSprintState(Entity target, CallbackInfo callback) {
        this.sprintDuringAttack = this.isSprinting();
    }

    // combatControl$onCriticalHit is taken from GoldenAgeCombat
    @SuppressWarnings("ConstantValue")
    @Inject(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            ))
    public void combatControl$onCriticalHit(Entity target, CallbackInfo callback) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(player);

        // allow landing critical hits when sprint jumping like before 1.9 and in combat test snapshots
        // the injection point is fine despite being inside a few conditions as the same conditions must apply for critical hits
        if (config.isPreventSprintCriticalHits()) return;

        // this disables sprinting, no need to call the dedicated method as it also updates the attribute modifier which is unnecessary since we reset the value anyway
        this.setFlag(3, false);
    }

    // combatControl$resetAttackSprintState is taken from GoldenAgeCombat
    @Inject(
            method = "attack",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;horizontalSpeed:F"
            )
    )
    public void combatControl$resetAttackSprintState(Entity target, CallbackInfo callback) {
        // reset to original sprinting value for rest of attack method
        if (this.sprintDuringAttack) this.setFlag(3, true);
    }

    // combatControl$handleAttackSprinting is taken from GoldenAgeCombat
    @SuppressWarnings("ConstantValue")
    @Inject(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V",
                    shift = At.Shift.AFTER
            )
    )
    public void combatControl$handleAttackSprinting(Entity target, CallbackInfo callback) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(player);

        // don't disable sprinting when attacking a target
        // this is mainly nice to have since you always stop to swim when attacking creatures underwater
        if (!config.isPreventAttackSprinting()) {
            if (this.sprintDuringAttack) this.setSprinting(true);
        }

        this.sprintDuringAttack = false;
    }
}
