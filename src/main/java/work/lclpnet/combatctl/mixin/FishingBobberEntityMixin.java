package work.lclpnet.combatctl.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.impl.CombatConfig;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin {

    @Shadow @Nullable public abstract PlayerEntity getPlayerOwner();

    @Inject(method = "onEntityHit", at = @At("TAIL"))
    protected void onHitEntity(EntityHitResult entityHitResult, CallbackInfo callback) {
        PlayerEntity player = getPlayerOwner();
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(serverPlayer);

        if (config.isPreventFishingRodKnockBack()) return;

        // for players, this is a weak attack; handled in PlayerEntityMixin#combatControl$onWeakDamage()
        FishingBobberEntity self = (FishingBobberEntity) (Object) this;
        entityHitResult.getEntity().damage(serverPlayer.getDamageSources().thrown(self, this.getPlayerOwner()), 0.0F);
    }
}
