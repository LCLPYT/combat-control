package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.impl.CombatConfig;

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

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(player);

        if (config.isAttackCooldown()) return;

        cir.setReturnValue(1.0F);
    }

    @SuppressWarnings("ConstantValue")
    @WrapWithCondition(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"
            )
    )
    public boolean combatControl$playCombatSoundsIfEnabled(World world, @Nullable PlayerEntity except, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return true;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(player);

        return config.isModernHitSounds();
    }
}
