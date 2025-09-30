package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
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
            method = "getToleranceMargin",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void combatControl$overrideToleranceMargin(Entity entity, CallbackInfoReturnable<Float> cir) {
        if (!(entity instanceof ProjectileEntity projectile) || !(projectile.getOwner() instanceof ServerPlayerEntity player)) return;

        PlayerConfig config = CombatControl.get(player.getEntityWorld().getServer()).playerConfig(player);

        if (config.isDynamicProjectileMargin()) return;

        // use constant 0.3 as in Minecraft 1.21.5 and before
        cir.setReturnValue(0.3f);
    }

    @WrapOperation(
            method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;F)Lnet/minecraft/util/hit/EntityHitResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/Box;raycast(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;)Ljava/util/Optional;"
            )
    )
    private static Optional<Vec3d> combatControl$overrideRaycast(Box instance, Vec3d from, Vec3d to, Operation<Optional<Vec3d>> original, @Local(argsOnly = true) Entity entity) {
        if (!(entity instanceof ProjectileEntity projectile) || !(projectile.getOwner() instanceof ServerPlayerEntity player)) {
            return original.call(instance, from, to);
        }

        PlayerConfig config = CombatControl.get(player.getEntityWorld().getServer()).playerConfig(player);

        if (config.isEarlyProjectileHits() && instance.contains(from)) {
            return Optional.of(from);
        }

        return original.call(instance, from, to);
    }
}
