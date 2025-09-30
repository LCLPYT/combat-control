package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.config.PlayerConfig;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {

    @Shadow
    public abstract @Nullable Entity getOwner();

    @ModifyExpressionValue(
            method = "canHit(Lnet/minecraft/entity/Entity;)Z",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/entity/projectile/ProjectileEntity;leftOwner:Z",
                    opcode = Opcodes.GETFIELD
            )
    )
    private boolean combatControl$modifyLeftOwner(boolean leftOwner, @Local(argsOnly = true) Entity entity) {
        if (!(getOwner() instanceof ServerPlayerEntity player) || leftOwner) return leftOwner;

        PlayerConfig config = CombatControl.get(player.getEntityWorld().getServer()).playerConfig(player);

        // allow hitting other entities inside the owner hitbox
        return config.isEarlyProjectileHits() && entity != (Object) this && entity != player;
    }
}
