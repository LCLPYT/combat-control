package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.impl.CombatConfig;

@Mixin(FishingRodItem.class)
public class FishingRodItemMixin {

    @Unique
    private PlayerEntity lastUser = null;

    @Inject(
            method = "use",
            at = @At("HEAD")
    )
    public void combatControl$beforeUse(World world, PlayerEntity user, Hand hand,
                                        CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        this.lastUser = user;
    }

    @WrapOperation(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"
            )
    )
    public void combatControl$onPlaySound(World world, @Nullable PlayerEntity except, double x, double y, double z,
                                          SoundEvent sound, SoundCategory category, float volume, float pitch,
                                          Operation<Void> original) {

        if (world.isClient || lastUser == null || !(lastUser instanceof ServerPlayerEntity serverPlayer)) {
            original.call(world, except, x, y, z, sound, category, volume, pitch);
            return;
        }

        CombatConfig config = CombatControl.get(lastUser.getServer()).getConfig(serverPlayer);

        if (config.isModernFishingRodSounds()) {
            original.call(world, except, x, y, z, sound, category, volume, pitch);
            return;
        }

        // skip all sounds but throw
        if (sound != SoundEvents.ENTITY_FISHING_BOBBER_THROW) return;

        // play low-pitched bow sound, as in the old version
        pitch = 0.4f / (serverPlayer.getRandom().nextFloat() * 0.4F + 0.8F);

        world.playSound(null, x, y, z, SoundEvents.ENTITY_ARROW_SHOOT, category, 0.5f, pitch);
    }
}
