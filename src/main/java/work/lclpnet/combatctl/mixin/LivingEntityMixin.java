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
    private void combatControl$modifyVelocity(LivingEntity instance, double x, double y, double z, Operation<Void> original,
                                              @Local(ordinal = 0) Vec3 velocity, @Local(ordinal = 1) Vec3 knockbackDir,
                                              @Local(ordinal = 0, argsOnly = true) double strength) {

        if (!(instance instanceof ServerPlayer player)
                || !KnockbackHandler.get(player.level().getServer()).applyKnockback(player, velocity, knockbackDir, strength)) {
            original.call(instance, x, y, z);
        }
    }
}
