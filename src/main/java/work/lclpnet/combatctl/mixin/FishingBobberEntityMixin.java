package work.lclpnet.combatctl.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
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

    @Inject(
            method = "onEntityHit", at = @At("TAIL"))
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
    public void retrieve(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        PlayerEntity player = getPlayerOwner();
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(serverPlayer);

        if (config.isModernFishingRodDurability()) return;

        if (callback.getReturnValueI() == 5) callback.setReturnValue(3);
    }
}
