package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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
import work.lclpnet.combatctl.hook.SwordBlockDamageCallback;
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
    public void combatControl$getAttackCooldownProgress(float a, CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        if (config.isAttackCooldown()) return;

        cir.setReturnValue(1.0F);
    }

    @SuppressWarnings("ConstantValue")
    @WrapWithCondition(
            method = {"attack", "attackVisualEffects"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;playServerSideSound(Lnet/minecraft/sounds/SoundEvent;)V"
            )
    )
    public boolean combatControl$playCombatSoundsIfEnabled(Player instance, SoundEvent sound) {
        if (!((Object) this instanceof ServerPlayer player)) return true;

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        return config.isModernHitSounds();
    }

    @SuppressWarnings("ConstantValue")
    @WrapWithCondition(
            method = "deflectProjectile",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;)V"
            )
    )
    public boolean combatControl$playDeflectSoundIfEnabled(Level instance, Entity except, double x, double y, double z, SoundEvent sound, SoundSource source) {
        if (!((Object) this instanceof ServerPlayer player)) return true;

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        return config.isModernHitSounds();
    }

    @SuppressWarnings("ConstantValue")
    @WrapWithCondition(
            method = "doSweepAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;playServerSideSound(Lnet/minecraft/sounds/SoundEvent;)V"
            )
    )
    public boolean combatControl$playSweepAttackSound(Player instance, SoundEvent sound) {
        if (!((Object) this instanceof ServerPlayer player)) return true;

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        if (config.isModernHitSounds()) {
            return true;
        }

        // trigger sweep attack sound if enabled or when the player has the sweeping edge enchantment on their weapon
        return config.isSweepAttack() || player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) > 0.0F;
    }

    @SuppressWarnings("ConstantValue")
    @WrapOperation(
            method = "damageStatsAndHearts",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"
            )
    )
    public int combatControl$spawnCombatParticlesIfEnabled(ServerLevel instance, ParticleOptions particle, double x, double y, double z, int count, double xDist, double yDist, double zDist, double speed, Operation<Integer> original) {
        if (particle != ParticleTypes.DAMAGE_INDICATOR || !((Object) this instanceof ServerPlayer player)) {
            return original.call(instance, particle, x, y, z, count, xDist, yDist, zDist, speed);
        }

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        if (config.isModernHitParticle()) {
            return original.call(instance, particle, x, y, z, count, xDist, yDist, zDist, speed);
        }

        return 0;
    }

    @SuppressWarnings("ConstantValue")
    @Inject(
            method = "doSweepAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"
            ),
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

    // modifies the "isSweepAttack" condition
    @SuppressWarnings("ConstantValue")
    @WrapOperation(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;isSweepAttack(ZZZ)Z"
            )
    )
    public boolean combatControl$modifyIsSweepAttack(Player instance, boolean fullStrengthAttack, boolean criticalAttack, boolean knockbackAttack, Operation<Boolean> original) {
        boolean originalValue = original.call(instance, fullStrengthAttack, criticalAttack, knockbackAttack);

        if (!originalValue) {
            return false;
        }

        if (!((Object) this instanceof ServerPlayer player)) {
            return true;
        }

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        if (config.isSweepAttack()) {
            return true;
        }

        // trigger sweep attack if enabled or when the player has the sweeping edge enchantment on their weapon
        return player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) > 0.0F;
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
    public void combatControl$onWeakDamage(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> callback) {
        if (!((Object) this instanceof ServerPlayer player)) return;

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        // check if weak attacks are enabled or if fishing rod knock back is enabled
        if (config.isNoWeakAttackKnockBack()
            && (config.isNoFishingRodKnockBack() || !(source.getDirectEntity() instanceof FishingHook))) return;

        if (Math.abs(damage) < 1e-9f && level().getDifficulty() != Difficulty.PEACEFUL) {
            callback.setReturnValue(super.hurtServer(level, source, damage));
        }
    }

    @SuppressWarnings("ConstantValue")
    @WrapOperation(
            method = "canCriticalAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;isSprinting()Z"
            )
    )
    public boolean combatControl$allowCritsWhileSprinting(Player instance, Operation<Boolean> original) {
        boolean sprinting = original.call(instance);

        if (!sprinting) {
            return false;
        }

        if (!((Object) this instanceof ServerPlayer player)) {
            return true;
        }

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        // allow landing critical hits when sprint jumping like before 1.9 and in combat test snapshots
        // pretend the player is not sprinting
        return config.isNoSprintCriticalHits();
    }

    // combatControl$handleAttackSprinting is taken from GoldenAgeCombat
    @SuppressWarnings("ConstantValue")
    @WrapOperation(
            method = "causeExtraKnockback",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"
            )
    )
    public void combatControl$handleAttackSprinting(Player instance, boolean b, Operation<Void> original) {
        // maybe add filter for non-stab attack in the future?

        if (!((Object) this instanceof ServerPlayer player)) {
            original.call(instance, b);
            return;
        }

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        // don't disable sprinting when attacking a target
        // this is mainly nice to have since you always stop to swim when attacking creatures underwater

        if (config.isNoAttackSprinting()) {
            // this disables sprinting as usually
            original.call(instance, b);
        }
    }

    @ModifyVariable(
            method = "actuallyHurt",
            at = @At(
                    value = "LOAD",
                    ordinal = 0
            ),
            argsOnly = true,
            name = "dmg"
    )
    private float combatControl$modifySwordBlockingDamage(float dmg, @Local(argsOnly = true, name = "source") DamageSource source) {
        Player self = (Player) (Object) this;

        if (!(self instanceof ServerPlayer player)) {
            return dmg;
        }

        if (!self.isUsingItem()) {
            return dmg;
        }

        ItemStack stack = player.getUseItem();

        if (stack == null || ToolInfo.of(stack).filter(ToolInfo::isSword).isEmpty()
                || DynamicItemHandler.getInstance().unhandled(stack, DynamicItemHandler.Property.SWORD_BLOCKING)) {
            return dmg;
        }

        if (!source.is(DamageTypeTags.BYPASSES_ARMOR) && dmg > 0.0F) {
            // damage reduction from 1.7.10
            float damage = (1.0F + dmg) * 0.5F;

            return SwordBlockDamageCallback.HOOK.invoker().calculateDamage(player, source, dmg, damage, stack);
        }

        return dmg;
    }
}
