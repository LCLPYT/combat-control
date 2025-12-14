package work.lclpnet.combatctl.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControlClient;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow @Nullable public LocalPlayer player;

    @Shadow @Final public Options options;

    @Shadow public int missTime;

    @Shadow @Nullable public HitResult hitResult;

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Nullable public MultiPlayerGameMode gameMode;

    @Shadow @Nullable public ClientLevel level;

    // combatControl$handleInputEvents is taken from GoldenAgeCombat
    @Inject(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z",
                    ordinal = 0
            )
    )
    public void combatControl$handleInputEvents(CallbackInfo callback) {
        // required for enabling block breaking while e.g. sword blocking
        // it is actually enabled by a different patch below, this just makes sure breaking particles show correctly (which only works sometimes otherwise)
        if (!CombatControlClient.get().abilities().attackWhileUsing || this.player == null || !this.player.isUsingItem()) return;

        while (this.options.keyAttack.consumeClick()) {
            this.combatControl$startBlockAttack();
        }
    }

    // combatControl$startBlockAttack is taken from GoldenAgeCombat
    @Unique
    private void combatControl$startBlockAttack() {
        if (this.missTime > 0) return;

        if (this.hitResult == null) {
            LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");

            if (this.gameMode != null && this.gameMode.hasMissTime()) {
                this.missTime = 10;
            }

            return;
        }

        if (this.player == null || this.level == null) return;

        ItemStack stack = this.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!stack.isItemEnabled(this.level.enabledFeatures()) || this.player.isHandsBusy()) return;

        if (this.hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockhitresult = (BlockHitResult) this.hitResult;
        BlockPos blockpos = blockhitresult.getBlockPos();

        if (!this.level.isEmptyBlock(blockpos)) {
            if (this.gameMode != null) {
                this.gameMode.startDestroyBlock(blockpos, blockhitresult.getDirection());
            }

            return;
        }

        this.player.swing(InteractionHand.MAIN_HAND);
    }

    @ModifyExpressionValue(
            method = "continueAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"
            )
    )
    public boolean combatControl$handleBlockBreaking(boolean original) {
        if (!CombatControlClient.get().abilities().attackWhileUsing) return original;

        return false;
    }

    @ModifyExpressionValue(
            method = "startUseItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying()Z"
            )
    )
    public boolean combatControl$startUseItem(boolean original) {
        if (!CombatControlClient.get().abilities().attackWhileUsing) return original;

        return false;
    }
}
