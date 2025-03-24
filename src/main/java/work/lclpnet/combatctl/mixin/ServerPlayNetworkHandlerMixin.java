package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.impl.PingHandler;
import work.lclpnet.combatctl.impl.SwordBlockingHandler;
import work.lclpnet.combatctl.type.CCServerPlayNetworkHandler;
import work.lclpnet.combatctl.type.ToolInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin implements CCServerPlayNetworkHandler {

    @Shadow public ServerPlayerEntity player;

    @Unique private final Object ccLock = new Object[0];
    @Unique private volatile PingHandler.Data pingData = null;

    @Inject(
            method = "onUpdateSelectedSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerInventory;getSelectedSlot()I"
            )
    )
    public void combatControl$capturePrevSelectedSlot(UpdateSelectedSlotC2SPacket packet, CallbackInfo ci,
                                                      @Share("wasSwordBlocking") LocalBooleanRef wasSwordBlocking) {
        if (!player.isUsingItem()) {
            wasSwordBlocking.set(false);
            return;
        }

        ItemStack stack = player.getActiveItem();

        if (stack == null || ToolInfo.of(stack).filter(ToolInfo::isSword).isEmpty()) {
            wasSwordBlocking.set(false);
            return;
        }

        boolean modded = CombatControl.get(player.getServer()).hasModInstalled(player);
        boolean offHand = player.getActiveHand() == Hand.OFF_HAND;

        if (modded && offHand) return;

        wasSwordBlocking.set(true);

        if (modded) return;

        int slot = offHand
                ? player.getInventory().getSelectedSlot()
                : PlayerInventory.OFF_HAND_SLOT;

        player.networkHandler.sendPacket(player.getInventory().createSlotSetPacket(slot));
    }

    @Inject(
            method = "onUpdateSelectedSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayerEntity;updateLastActionTime()V"
            )
    )
    public void combatControl$clearTemporaryShield(UpdateSelectedSlotC2SPacket packet, CallbackInfo ci,
                                                   @Share("wasSwordBlocking") LocalBooleanRef wasSwordBlocking) {
        if (!wasSwordBlocking.get()) return;

        // remove fake shield for other vanilla players after active slot was updated
        var control = CombatControl.get(player.getServer());
        SwordBlockingHandler.sendToNearbyVanillaPlayers(player, control, SwordBlockingHandler.fakeShieldUnequipPacket(player), false);
    }

    @Override
    public PingHandler.@NotNull Data combatControl$getPingData() {
        if (pingData != null) {
            return pingData;
        }

        synchronized (ccLock) {
            if (pingData == null) {
                pingData = new PingHandler.Data();
            }
        }

        return pingData;
    }
}
