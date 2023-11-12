package work.lclpnet.combatctl.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
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

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow @Nullable public ClientPlayerEntity player;

    @Shadow @Final public GameOptions options;

    @Shadow public int attackCooldown;

    @Shadow @Nullable public HitResult crosshairTarget;

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;

    @Shadow @Nullable public ClientWorld world;

    // combatControl$handleInputEvents is taken from GoldenAgeCombat
    @Inject(
            method = "handleInputEvents",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z",
                    ordinal = 0
            )
    )
    public void combatControl$handleInputEvents(CallbackInfo callback) {
        // required for enabling block breaking while e.g. sword blocking
        // it is actually enabled by a different patch below, this just makes sure breaking particles show correctly (which only works sometimes otherwise)
        if (!CombatControlClient.get().getAbilities().attackWhileUsing || this.player == null || !this.player.isUsingItem()) return;

        while (this.options.attackKey.wasPressed()) {
            this.combatControl$startBlockAttack();
        }
    }

    // combatControl$startBlockAttack is taken from GoldenAgeCombat
    @Unique
    private void combatControl$startBlockAttack() {
        if (this.attackCooldown > 0) return;

        if (this.crosshairTarget == null) {
            LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");

            if (this.interactionManager != null && this.interactionManager.hasLimitedAttackSpeed()) {
                this.attackCooldown = 10;
            }

            return;
        }

        if (this.player == null || this.world == null) return;

        ItemStack stack = this.player.getStackInHand(Hand.MAIN_HAND);
        if (!stack.isItemEnabled(this.world.getEnabledFeatures()) || this.player.isRiding()) return;

        if (this.crosshairTarget.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockhitresult = (BlockHitResult) this.crosshairTarget;
        BlockPos blockpos = blockhitresult.getBlockPos();

        if (!this.world.isAir(blockpos)) {
            if (this.interactionManager != null) {
                this.interactionManager.attackBlock(blockpos, blockhitresult.getSide());
            }

            return;
        }

        this.player.swingHand(Hand.MAIN_HAND);
    }

    @ModifyExpressionValue(
            method = "handleBlockBreaking",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
            )
    )
    public boolean combatControl$handleBlockBreaking(boolean original) {
        if (!CombatControlClient.get().getAbilities().attackWhileUsing) return original;

        return false;
    }

    @ModifyExpressionValue(
            method = "doItemUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;isBreakingBlock()Z"
            )
    )
    public boolean combatControl$startUseItem(boolean original) {
        if (!CombatControlClient.get().getAbilities().attackWhileUsing) return original;

        return false;
    }
}
