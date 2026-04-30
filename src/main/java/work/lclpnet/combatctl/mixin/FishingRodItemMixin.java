package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.config.PlayerConfig;

@Mixin(FishingRodItem.class)
public class FishingRodItemMixin {

    @WrapOperation(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"
            )
    )
    public void combatControl$onPlaySound(Level instance, Entity except, double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch, Operation<Void> original, @Local(argsOnly = true, name = "player") Player player) {

        if (instance.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            original.call(instance, except, x, y, z, sound, source, volume, pitch);
            return;
        }

        PlayerConfig config = CombatControl.get(serverPlayer.level().getServer()).playerConfig(serverPlayer);

        if (config.isModernFishingRodSounds()) {
            original.call(instance, except, x, y, z, sound, source, volume, pitch);
            return;
        }

        // skip all sounds but throw
        if (sound != SoundEvents.FISHING_BOBBER_THROW) return;

        // play low-pitched bow sound, as in the old version
        pitch = 0.4f / (serverPlayer.getRandom().nextFloat() * 0.4F + 0.8F);

        instance.playSound(null, x, y, z, SoundEvents.ARROW_SHOOT, source, 0.5f, pitch);
    }
}
