package work.lclpnet.combatctl.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.config.PlayerConfig;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {

    @Inject(
            method = "getToleranceMargin",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void combatControl$overrideToleranceMargin(Entity entity, CallbackInfoReturnable<Float> cir) {
        if (!(entity instanceof ProjectileEntity projectile) || !(projectile.getOwner() instanceof ServerPlayerEntity player)) return;

        PlayerConfig config = CombatControl.get(player.getServer()).playerConfig(player);

        if (config.isDynamicProjectileMargin()) return;

        // use constant 0.3 as in Minecraft 1.21.5 and before
        cir.setReturnValue(0.3f);
    }
}
