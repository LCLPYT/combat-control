package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.config.PlayerConfig;
import work.lclpnet.combatctl.impl.DynamicItemHandler;
import work.lclpnet.combatctl.type.ToolInfo;

@SuppressWarnings("UnreachableCode")
@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @SuppressWarnings("ConstantValue")
    @Inject(
            method = "getAttackStrengthScale",
            at = @At("HEAD"),
            cancellable = true
    )
    public void combatControl$getAttackCooldownProgress(float baseTime, CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        if (config.isAttackCooldown()) return;

        cir.setReturnValue(1.0F);
    }

    @SuppressWarnings("ConstantValue")
    @WrapWithCondition(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"
            )
    )
    public boolean combatControl$playCombatSoundsIfEnabled(Level instance, Entity source, double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch) {
        if (!((Object) this instanceof ServerPlayer player)) return true;

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        if (config.isModernHitSounds()) {
            return true;
        }

        if (sound != SoundEvents.PLAYER_ATTACK_SWEEP) {
            return false;
        }

        // trigger sweep attack sound if enabled or when the player has the sweeping edge enchantment on their weapon
        return config.isSweepAttack() || player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) > 0.0F;
    }

    @SuppressWarnings("ConstantValue")
    @WrapOperation(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"
            )
    )
    public int combatControl$spawnCombatParticlesIfEnabled(ServerLevel instance, ParticleOptions particle, double x, double y, double z, int count, double deltaX, double deltaY, double deltaZ, double speed, Operation<Integer> original) {
        if (particle != ParticleTypes.DAMAGE_INDICATOR || !((Object) this instanceof ServerPlayer player)) {
            return original.call(instance, particle, x, y, z, count, deltaX, deltaY, deltaZ, speed);
        }

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        if (config.isModernHitParticle()) {
            return original.call(instance, particle, x, y, z, count, deltaX, deltaY, deltaZ, speed);
        }

        return 0;
    }

    @SuppressWarnings("ConstantValue")
    @Inject(
            method = "sweepAttack",
            at = @At("HEAD"),
            cancellable = true
    )
    public void combatControl$spawnSweepAttackParticles(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayer player)) return;

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        // trigger sweep attack particle if enabled or when the player has the sweeping edge enchantment on their weapon
        if (config.isSweepAttack() || player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) > 0.0F) {
            return;
        }

        ci.cancel();
    }

    @SuppressWarnings("ConstantValue")
    @ModifyVariable(method = "attack", at = @At("LOAD"), ordinal = 3, slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;sweepAttack()V")))
    public boolean combatControl$modifySweepAttack(boolean original, Entity target) {
        if (!((Object) this instanceof ServerPlayer player)) return original;

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        if (config.isSweepAttack()) return original;

        // trigger sweep attack if enabled or when the player has the sweeping edge enchantment on their weapon
        return original && player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) > 0.0F;
    }

    @SuppressWarnings("ConstantValue")
    @Inject(
            method = "hurtServer",
            at = @At(
                    value = "RETURN",
                    ordinal = 0
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/damagesource/DamageSource;scalesWithDifficulty()Z"
                    )
            ),
            cancellable = true
    )
    public void combatControl$onWeakDamage(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
        if (!((Object) this instanceof ServerPlayer player)) return;

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        // check if weak attacks are enabled or if fishing rod knock back is enabled
        if (config.isNoWeakAttackKnockBack()
            && (config.isNoFishingRodKnockBack() || !(source.getDirectEntity() instanceof FishingHook))) return;

        if (Math.abs(amount) < 1e-9f && level().getDifficulty() != Difficulty.PEACEFUL) {
            callback.setReturnValue(super.hurtServer(world, source, amount));
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
                    target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            ))
    public void combatControl$onCriticalHit(Entity target, CallbackInfo callback) {
        if (!((Object) this instanceof ServerPlayer player)) return;

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        // allow landing critical hits when sprint jumping like before 1.9 and in combat test snapshots
        // the injection point is fine despite being inside a few conditions as the same conditions must apply for critical hits
        if (config.isNoSprintCriticalHits()) return;

        // this disables sprinting, no need to call the dedicated method as it also updates the attribute modifier which is unnecessary since we reset the value anyway
        this.setSharedFlag(3, false);
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
            this.setSharedFlag(3, true);
        }

        return b;  // we never modify the variable, this injection is used as a means to reset the sprinting state ASAP
    }

    // combatControl$handleAttackSprinting is taken from GoldenAgeCombat
    @SuppressWarnings("ConstantValue")
    @Inject(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V",
                    shift = At.Shift.AFTER
            )
    )
    public void combatControl$handleAttackSprinting(Entity target, CallbackInfo callback, @Share("sprintDuringAttack") LocalBooleanRef sprintDuringAttack) {
        if (!((Object) this instanceof ServerPlayer player)) return;

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        // don't disable sprinting when attacking a target
        // this is mainly nice to have since you always stop to swim when attacking creatures underwater
        if (!config.isNoAttackSprinting() && sprintDuringAttack.get()) {
            this.setSprinting(true);
        }
    }

    @ModifyVariable(
            method = "actuallyHurt",
            at = @At(
                    value = "LOAD",
                    ordinal = 0
            ),
            argsOnly = true
    )
    private float combatControl$modifySwordBlockingDamage(float amount, @Local(argsOnly = true) DamageSource source) {
        Player self = (Player) (Object) this;

        if (!self.isUsingItem()) {
            return amount;
        }

        ItemStack stack = self.getUseItem();


        if (stack == null || ToolInfo.of(stack).filter(ToolInfo::isSword).isEmpty()
                || DynamicItemHandler.getInstance().unhandled(stack, DynamicItemHandler.Property.SWORD_BLOCKING)) {
            return amount;
        }

        // damage reduction from 1.7.10
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR) && amount > 0.0F) {
            return (1.0F + amount) * 0.5F;
        }

        return amount;
    }
}
