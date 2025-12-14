package work.lclpnet.combatctl.mixin.client;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.projectile.Snowball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControlClient;

@Mixin(Snowball.class)
public class SnowballMixin {

    @Inject(
            method = "getParticle",
            at = @At("RETURN"),
            cancellable = true
    )
    public void combatControl$modifyParticle(CallbackInfoReturnable<ParticleOptions> cir) {
        if (CombatControlClient.get().config().isClassicSnowballParticle()) {
            cir.setReturnValue(ParticleTypes.ITEM_SNOWBALL);
        }
    }
}
