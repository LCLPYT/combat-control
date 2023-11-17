package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.impl.CombatConfig;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin {

    @Shadow @Nullable public abstract PlayerEntity getPlayerOwner();
    @Shadow @Final private net.minecraft.util.math.random.Random velocityRandom;

    @Inject(
            method = "onEntityHit",
            at = @At("TAIL")
    )
    protected void combatControl$onHitEntity(EntityHitResult entityHitResult, CallbackInfo callback) {
        PlayerEntity player = getPlayerOwner();
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(serverPlayer);

        if (config.isNoFishingRodKnockBack()) return;

        // for players, this is a weak attack; handled in PlayerEntityMixin#combatControl$onWeakDamage()
        FishingBobberEntity self = (FishingBobberEntity) (Object) this;
        entityHitResult.getEntity().damage(serverPlayer.getDamageSources().thrown(self, this.getPlayerOwner()), 0.0F);
    }

    // combatControl$pullHookedEntity is taken from GoldenAgeCombat
    @Inject(
            method = "pullHookedEntity",
            at = @At("HEAD"),
            cancellable = true
    )
    protected void combatControl$pullHookedEntity(Entity entity, CallbackInfo callback) {
        PlayerEntity player = getPlayerOwner();
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(serverPlayer);

        if (!config.isFishingRodLaunch()) return;

        FishingBobberEntity self = (FishingBobberEntity) (Object) this;

        Vec3d vec3 = new Vec3d(player.getX() - self.getX(), player.getY() - self.getY(), player.getZ() - self.getZ()).multiply(0.1);
        Vec3d deltaMovement = entity.getVelocity();
        // values taken from Minecraft 1.8
        double x = deltaMovement.getX() * 10.0, y = deltaMovement.getY() * 10.0, z = deltaMovement.getZ() * 10.0;
        deltaMovement = deltaMovement.add(0.0, Math.pow(x * x + y * y + z * z, 0.25) * 0.08, 0.0);
        entity.setVelocity(deltaMovement.add(vec3));
        callback.cancel();
    }

    @Inject(
            method = "use",
            at = @At("RETURN"),
            cancellable = true
    )
    public void combatControl$retrieve(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        PlayerEntity player = getPlayerOwner();
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(serverPlayer);

        if (config.isModernFishingRodDurability()) return;

        if (callback.getReturnValueI() == 5) callback.setReturnValue(3);
    }

    @WrapOperation(
            method = "<init>(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/FishingBobberEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"
            )
    )
    public void combatControl$setVelocity(FishingBobberEntity instance, Vec3d velocity, Operation<Void> original) {
        PlayerEntity player = getPlayerOwner();
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(serverPlayer);

        if (config.isSlowFishingRodMotion()) {
            original.call(instance, velocity);
            return;
        }

        FishingBobberEntity self = (FishingBobberEntity) (Object) this;

        float yaw = self.getYaw();
        float pitch = self.getPitch();

        float initialStrength = 0.4f, amplifier = 1.0f, strength = 1.5f;

        double vx = -MathHelper.sin(yaw / 180.0F * (float)Math.PI) * MathHelper.cos(pitch / 180.0F * (float)Math.PI) * initialStrength;
        double vy = MathHelper.cos(yaw / 180.0F * (float)Math.PI) * MathHelper.cos(pitch / 180.0F * (float)Math.PI) * initialStrength;
        double vz = -MathHelper.sin(pitch / 180.0F * (float)Math.PI) * initialStrength;

        double len = Math.sqrt(vx * vx + vz * vz + vy * vy);

        vx = (vx / len + velocityRandom.nextGaussian() * 0.007499999832361937D * amplifier) * strength;
        vz = (vz / len + velocityRandom.nextGaussian() * 0.007499999832361937D * amplifier) * strength;
        vy = (vy / len + velocityRandom.nextGaussian() * 0.007499999832361937D * amplifier) * strength;

        Vec3d vel = new Vec3d(vx, vz, vy);
        self.setVelocity(vel);
    }

    @Inject(
            method = "<init>(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;II)V",
            at = @At("TAIL")
    )
    public void combatControl$postConstruct(PlayerEntity thrower, World world, int luckOfTheSeaLevel, int lureLevel, CallbackInfo ci) {
        PlayerEntity player = getPlayerOwner();
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(serverPlayer);

        if (config.isSlowFishingRodMotion()) return;

        FishingBobberEntity self = (FishingBobberEntity) (Object) this;
        Vec3d vel = self.getVelocity();

        double vx = vel.getX(), vy = vel.getY(), vz = vel.getZ();
        double len2d = Math.sqrt(vx * vx + vz * vz);

        float yaw = (float) (MathHelper.atan2(vx, vz) * 180.0D / Math.PI);
        self.setYaw(yaw);
        self.prevYaw = yaw;

        float pitch = (float)(MathHelper.atan2(vy, len2d) * 180.0D / Math.PI);
        self.setPitch(pitch);
        self.prevPitch = pitch;
    }
}
