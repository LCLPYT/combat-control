package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.config.PlayerConfig;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {

    @Shadow
    public abstract @Nullable Entity getOwner();

    @ModifyExpressionValue(
            method = "canHitEntity(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/projectile/Projectile;leftOwner:Z",
                    opcode = Opcodes.GETFIELD
            )
    )
    private boolean combatControl$modifyLeftOwner(boolean leftOwner, @Local(argsOnly = true, name = "entity") Entity entity) {
        if (!(getOwner() instanceof ServerPlayer player) || leftOwner) return leftOwner;

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        // allow hitting other entities inside the owner hitbox
        return config.isEarlyProjectileHits() && entity != (Object) this && entity != player;
    }
}
