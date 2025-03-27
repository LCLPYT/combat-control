package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import work.lclpnet.combatctl.impl.KnockbackHandler;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @WrapOperation(
            method = "takeKnockback",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;setVelocity(DDD)V"
            )
    )
    private void combatControl$modifyVelocity(LivingEntity instance, double x, double y, double z, Operation<Void> original,
                                              @Local(ordinal = 0) Vec3d velocity, @Local(ordinal = 1) Vec3d knockbackDir,
                                              @Local(ordinal = 0, argsOnly = true) double strength) {

        if (!(instance instanceof ServerPlayerEntity player)
                || !KnockbackHandler.get(player.getServer()).applyKnockback(player, velocity, knockbackDir, strength)) {
            original.call(instance, x, y, z);
        }
    }
}
