package work.lclpnet.combatctl.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.impl.CombatDetails;
import work.lclpnet.combatctl.impl.enums.Status;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @SuppressWarnings("ConstantValue")
    @Inject(
            method = "getAttackCooldownProgress",
            at = @At("HEAD"),
            cancellable = true
    )
    public void combatControl$getAttackCooldownProgress(float baseTime, CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;

        Status status = CombatControl.getInstance().getDetail(player, CombatDetails.ATTACK_COOLDOWN);

        if (status == Status.ENABLED) return;

        cir.setReturnValue(1.0F);
    }
}
