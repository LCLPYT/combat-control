package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import work.lclpnet.combatctl.impl.KnockbackHandler;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @WrapOperation(
            method = "knockback",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V"
            )
    )
    private void combatControl$modifyVelocity(
            LivingEntity instance,
            double x,
            double y,
            double z,
            Operation<Void> original,
            @Local(name = "deltaMovement") Vec3 deltaMovement,
            @Local(name = "deltaVector") Vec3 deltaVector,
            @Local(argsOnly = true, name = "power") double power
    ) {
        if (!(instance instanceof ServerPlayer player)
                || !KnockbackHandler.get(player.level().getServer()).applyKnockback(player, deltaMovement, deltaVector, power)) {
            original.call(instance, x, y, z);
        }
    }
}
