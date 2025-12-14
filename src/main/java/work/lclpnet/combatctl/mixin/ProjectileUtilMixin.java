package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.config.PlayerConfig;

import java.util.Optional;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {

    @Inject(
            method = "computeMargin",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void combatControl$overrideToleranceMargin(Entity entity, CallbackInfoReturnable<Float> cir) {
        if (!(entity instanceof Projectile projectile) || !(projectile.getOwner() instanceof ServerPlayer player)) return;

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        if (config.isDynamicProjectileMargin()) return;

        // use constant 0.3 as in Minecraft 1.21.5 and before
        cir.setReturnValue(0.3f);
    }

    @WrapOperation(
            method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;"
            )
    )
    private static Optional<Vec3> combatControl$overrideRaycast(AABB instance, Vec3 from, Vec3 to, Operation<Optional<Vec3>> original, @Local(argsOnly = true) Entity entity) {
        if (!(entity instanceof Projectile projectile) || !(projectile.getOwner() instanceof ServerPlayer player)) {
            return original.call(instance, from, to);
        }

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        if (config.isEarlyProjectileHits() && instance.contains(from)) {
            return Optional.of(from);
        }

        return original.call(instance, from, to);
    }
}
