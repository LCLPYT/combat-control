package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
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
                    target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"
            )
    )
    public void combatControl$onPlaySound(World world, @Nullable PlayerEntity except, double x, double y, double z,
                                          SoundEvent sound, SoundCategory category, float volume, float pitch,
                                          Operation<Void> original, @Local(argsOnly = true) PlayerEntity user) {

        if (world.isClient || !(user instanceof ServerPlayerEntity player)) {
            original.call(world, except, x, y, z, sound, category, volume, pitch);
            return;
        }

        PlayerConfig config = CombatControl.get(player.getServer()).playerConfig(player);

        if (config.isModernFishingRodSounds()) {
            original.call(world, except, x, y, z, sound, category, volume, pitch);
            return;
        }

        // skip all sounds but throw
        if (sound != SoundEvents.ENTITY_FISHING_BOBBER_THROW) return;

        // play low-pitched bow sound, as in the old version
        pitch = 0.4f / (player.getRandom().nextFloat() * 0.4F + 0.8F);

        world.playSound(null, x, y, z, SoundEvents.ENTITY_ARROW_SHOOT, category, 0.5f, pitch);
    }
}
