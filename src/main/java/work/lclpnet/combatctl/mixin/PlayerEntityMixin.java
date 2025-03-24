package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.config.PlayerConfig;
import work.lclpnet.combatctl.type.ToolInfo;

@SuppressWarnings("UnreachableCode")
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

        PlayerConfig config = CombatControl.get(player.getServer()).playerConfig(player);

        if (config.isAttackCooldown()) return;

        cir.setReturnValue(1.0F);
    }

    @SuppressWarnings("ConstantValue")
    @WrapWithCondition(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/Entity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"
            )
    )
    public boolean combatControl$playCombatSoundsIfEnabled(World instance, Entity source, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return true;

        PlayerConfig config = CombatControl.get(player.getServer()).playerConfig(player);

        if (config.isModernHitSounds()) {
            return true;
        }

        if (sound != SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP) {
            return false;
        }

        // trigger sweep attack sound if enabled or when the player has the sweeping edge enchantment on their weapon
        return config.isSweepAttack() || player.getAttributeValue(EntityAttributes.SWEEPING_DAMAGE_RATIO) > 0.0F;
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

        PlayerConfig config = CombatControl.get(player.getServer()).playerConfig(player);

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

        PlayerConfig config = CombatControl.get(player.getServer()).playerConfig(player);

        // trigger sweep attack particle if enabled or when the player has the sweeping edge enchantment on their weapon
        if (config.isSweepAttack() || player.getAttributeValue(EntityAttributes.SWEEPING_DAMAGE_RATIO) > 0.0F) {
            return;
        }

        ci.cancel();
    }

    @SuppressWarnings("ConstantValue")
    @ModifyVariable(method = "attack", at = @At("LOAD"), ordinal = 3, slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;spawnSweepAttackParticles()V")))
    public boolean combatControl$modifySweepAttack(boolean original, Entity target) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return original;

        PlayerConfig config = CombatControl.get(player.getServer()).playerConfig(player);

        if (config.isSweepAttack()) return original;

        // trigger sweep attack if enabled or when the player has the sweeping edge enchantment on their weapon
        return original && player.getAttributeValue(EntityAttributes.SWEEPING_DAMAGE_RATIO) > 0.0F;
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
    public void combatControl$onWeakDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;

        PlayerConfig config = CombatControl.get(player.getServer()).playerConfig(player);

        // check if weak attacks are enabled or if fishing rod knock back is enabled
        if (config.isNoWeakAttackKnockBack()
            && (config.isNoFishingRodKnockBack() || !(source.getSource() instanceof FishingBobberEntity))) return;

        if (Math.abs(amount) < 1e-9f && getWorld().getDifficulty() != Difficulty.PEACEFUL) {
            callback.setReturnValue(super.damage(world, source, amount));
        }
    }

    // combatControl$initialAttackSprintState is taken from GoldenAgeCombat
    @Inject(method = "attack", at = @At("HEAD"))
    public void combatControl$initialAttackSprintState(Entity target, CallbackInfo callback, @Share("sprintDuringAttack") LocalBooleanRef sprintDuringAttack) {
        sprintDuringAttack.set(this.isSprinting());
    }

    // combatControl$onCriticalHit is taken from GoldenAgeCombat
    @SuppressWarnings("ConstantValue")
    @Inject(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/Entity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            ))
    public void combatControl$onCriticalHit(Entity target, CallbackInfo callback) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;

        PlayerConfig config = CombatControl.get(player.getServer()).playerConfig(player);

        // allow landing critical hits when sprint jumping like before 1.9 and in combat test snapshots
        // the injection point is fine despite being inside a few conditions as the same conditions must apply for critical hits
        if (config.isNoSprintCriticalHits()) return;

        // this disables sprinting, no need to call the dedicated method as it also updates the attribute modifier which is unnecessary since we reset the value anyway
        this.setFlag(3, false);
    }

    // combatControl$resetAttackSprintState is taken from GoldenAgeCombat
    @ModifyVariable(
            method = "attack",
            at = @At(
                    value = "STORE",
                    ordinal = 0
            ),
            index = 11  // inject after first ISTORE 11 instruction bl4 = false (LVT index 11 is boolean bl4) around line 1212 in version 1.21.4
    )
    public boolean combatControl$resetAttackSprintState(boolean b, @Share("sprintDuringAttack") LocalBooleanRef sprintDuringAttack) {
        // reset to original sprinting value for rest of attack method
        // this injection should be shortly after the !isSprinting check, but must not be conditional
        if (sprintDuringAttack.get()) {
            this.setFlag(3, true);
        }

        return b;  // we never modify the variable, this injection is used as a means to reset the sprinting state ASAP
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
    public void combatControl$handleAttackSprinting(Entity target, CallbackInfo callback, @Share("sprintDuringAttack") LocalBooleanRef sprintDuringAttack) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;

        PlayerConfig config = CombatControl.get(player.getServer()).playerConfig(player);

        // don't disable sprinting when attacking a target
        // this is mainly nice to have since you always stop to swim when attacking creatures underwater
        if (!config.isNoAttackSprinting() && sprintDuringAttack.get()) {
            this.setSprinting(true);
        }
    }

    @ModifyVariable(
            method = "applyDamage",
            at = @At(
                    value = "LOAD",
                    ordinal = 0
            ),
            argsOnly = true
    )
    private float combatControl$modifySwordBlockingDamage(float amount, @Local(argsOnly = true) DamageSource source) {
        PlayerEntity self = (PlayerEntity) (Object) this;

        if (!self.isUsingItem()) {
            return amount;
        }

        ItemStack stack = self.getActiveItem();

        if (stack == null || ToolInfo.of(stack).filter(ToolInfo::isSword).isEmpty()) {
            return amount;
        }

        // damage reduction from 1.7.10
        if (!source.isIn(DamageTypeTags.BYPASSES_ARMOR) && amount > 0.0F) {
            return (1.0F + amount) * 0.5F;
        }

        return amount;
    }
}
