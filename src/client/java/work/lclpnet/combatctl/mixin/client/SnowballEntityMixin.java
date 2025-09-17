package work.lclpnet.combatctl.mixin.client;

import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControlClient;

@Mixin(SnowballEntity.class)
public class SnowballEntityMixin {

    @Inject(
            method = "getParticleParameters",
            at = @At("RETURN"),
            cancellable = true
    )
    public void combatControl$modifyParticle(CallbackInfoReturnable<ParticleEffect> cir) {
        if (CombatControlClient.get().config().isClassicSnowballParticle()) {
            cir.setReturnValue(ParticleTypes.ITEM_SNOWBALL);
        }
    }
}
