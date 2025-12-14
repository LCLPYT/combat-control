package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.config.PlayerConfig;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {

    @Shadow @Nullable public abstract Player getPlayerOwner();
    @Shadow @Final private net.minecraft.util.RandomSource syncronizedRandom;

    @Inject(
            method = "onHitEntity",
            at = @At("TAIL")
    )
    protected void combatControl$onHitEntity(EntityHitResult entityHitResult, CallbackInfo callback) {
        if (!(getPlayerOwner() instanceof ServerPlayer player)) return;

        PlayerConfig config = combatConfig();

        if (config == null || config.isNoFishingRodKnockBack()) return;

        // for players, this is a weak attack; handled in PlayerEntityMixin#combatControl$onWeakDamage()
        FishingHook self = (FishingHook) (Object) this;
        entityHitResult.getEntity().hurtServer(player.level(), player.damageSources().thrown(self, this.getPlayerOwner()), 0.0F);
    }

    // combatControl$pullHookedEntity is taken from GoldenAgeCombat
    @Inject(
            method = "pullEntity",
            at = @At("HEAD"),
            cancellable = true
    )
    protected void combatControl$pullHookedEntity(Entity entity, CallbackInfo callback) {
        PlayerConfig config = combatConfig();
        Player player = getPlayerOwner();

        if (player == null || config == null || !config.isFishingRodLaunch()) return;

        FishingHook self = (FishingHook) (Object) this;

        Vec3 vec3 = new Vec3(player.getX() - self.getX(), player.getY() - self.getY(), player.getZ() - self.getZ()).scale(0.1);
        Vec3 deltaMovement = entity.getDeltaMovement();
        // values taken from Minecraft 1.8
        double x = deltaMovement.x() * 10.0, y = deltaMovement.y() * 10.0, z = deltaMovement.z() * 10.0;
        deltaMovement = deltaMovement.add(0.0, Math.pow(x * x + y * y + z * z, 0.25) * 0.08, 0.0);
        entity.setDeltaMovement(deltaMovement.add(vec3));
        callback.cancel();
    }

    @Inject(
            method = "retrieve",
            at = @At("RETURN"),
            cancellable = true
    )
    public void combatControl$retrieve(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        PlayerConfig config = combatConfig();

        if (config == null || config.isModernFishingRodDurability()) return;

        if (callback.getReturnValueI() == 5) callback.setReturnValue(3);
    }

    @WrapOperation(
            method = "<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/FishingHook;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
            )
    )
    public void combatControl$setVelocity(FishingHook instance, Vec3 velocity, Operation<Void> original) {
        PlayerConfig config = combatConfig();

        if (config == null) return;

        if (config.isSlowFishingRodMotion()) {
            original.call(instance, velocity);
            return;
        }

        FishingHook self = (FishingHook) (Object) this;

        float yaw = self.getYRot();
        float pitch = self.getXRot();

        float initialStrength = 0.4f, amplifier = 1.0f, strength = 1.5f;

        double vx = -Mth.sin(yaw / 180.0F * (float)Math.PI) * Mth.cos(pitch / 180.0F * (float)Math.PI) * initialStrength;
        double vy = Mth.cos(yaw / 180.0F * (float)Math.PI) * Mth.cos(pitch / 180.0F * (float)Math.PI) * initialStrength;
        double vz = -Mth.sin(pitch / 180.0F * (float)Math.PI) * initialStrength;

        double len = Math.sqrt(vx * vx + vz * vz + vy * vy);

        vx = (vx / len + syncronizedRandom.nextGaussian() * 0.0075D * amplifier) * strength;
        vz = (vz / len + syncronizedRandom.nextGaussian() * 0.0075D * amplifier) * strength;
        vy = (vy / len + syncronizedRandom.nextGaussian() * 0.0075D * amplifier) * strength;

        Vec3 vel = new Vec3(vx, vz, vy);
        self.setDeltaMovement(vel);
    }

    @Inject(
            method = "<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;II)V",
            at = @At("TAIL")
    )
    public void combatControl$postConstruct(Player thrower, Level world, int luckOfTheSeaLevel, int lureLevel, CallbackInfo ci) {
        PlayerConfig config = combatConfig();

        if (config == null || config.isSlowFishingRodMotion()) return;

        FishingHook self = (FishingHook) (Object) this;
        Vec3 vel = self.getDeltaMovement();

        double vx = vel.x(), vy = vel.y(), vz = vel.z();
        double len2d = Math.sqrt(vx * vx + vz * vz);

        float yaw = (float) (Mth.atan2(vx, vz) * 180.0D / Math.PI);
        self.setYRot(yaw);
        self.yRotO = yaw;

        float pitch = (float)(Mth.atan2(vy, len2d) * 180.0D / Math.PI);
        self.setXRot(pitch);
        self.xRotO = pitch;
    }

    @WrapWithCondition(
            method = "retrieve",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/FishingHook;pullEntity(Lnet/minecraft/world/entity/Entity;)V"
            )
    )
    public boolean combatControl$wrapPullHookedEntity(FishingHook instance, Entity entity) {
        PlayerConfig config = combatConfig();
        return config == null || config.isFishingRodPull();
    }

    @WrapWithCondition(
            method = "retrieve",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;broadcastEntityEvent(Lnet/minecraft/world/entity/Entity;B)V"
            )
    )
    public boolean combatControl$wrapSendPulledStatus(Level instance, Entity entity, byte status) {
        PlayerConfig config = combatConfig();
        return config == null || config.isFishingRodPull();
    }

    @Unique @Nullable
    private PlayerConfig combatConfig() {
        if (!(getPlayerOwner() instanceof ServerPlayer player)) return null;

        return CombatControl.get(player.level().getServer()).playerConfig(player);
    }
}
