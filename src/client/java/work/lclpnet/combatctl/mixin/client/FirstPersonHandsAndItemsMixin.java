package work.lclpnet.combatctl.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControlClient;

@Mixin(FirstPersonHandsAndItems.class)
public class FirstPersonHandsAndItemsMixin {

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"
            )
    )
    public float combatControl$removeCooldownEquipAnimation(LocalPlayer instance, float v, Operation<Float> original) {
        if (CombatControlClient.get().abilities().attackCooldown) {
            return original.call(instance, v);
        }

        return 1;
    }

    @Inject(
            method = "itemUsed",
            at = @At("HEAD"),
            cancellable = true
    )
    public void combatControl$onResetEquipProgress(InteractionHand hand, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;

        if (CombatControlClient.get().abilities().noReequipWhenUsing
                && player != null && player.isUsingItem() && player.getUsedItemHand() == hand) {
            ci.cancel();
        }
    }
}
