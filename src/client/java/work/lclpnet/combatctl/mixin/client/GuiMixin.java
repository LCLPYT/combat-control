package work.lclpnet.combatctl.mixin.client;

import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControlClient;
import work.lclpnet.combatctl.network.CombatAbilities;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Shadow @Final private Minecraft minecraft;

    @Unique
    private final CombatAbilities combatAbilities = CombatControlClient.get().abilities();
    @Unique
    @Nullable
    private static AttackIndicatorStatus attackIndicator = null;

    @Inject(
            method = "renderCrosshair",
            at = @At("HEAD")
    )
    public void combatControl$beforeRenderCrossHair(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (combatAbilities.attackCooldown) return;

        // functionality from GoldenAgeCombat
        if (attackIndicator == null) {
            var option = minecraft.options.attackIndicator();
            attackIndicator = option.get();
            option.set(AttackIndicatorStatus.OFF);
        }
    }

    @Inject(
            method = "renderCrosshair",
            at = @At("TAIL")
    )
    public void combatControl$afterRenderCrossHair(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        // functionality from GoldenAgeCombat
        if (attackIndicator != null) {
            minecraft.options.attackIndicator().set(attackIndicator);
            attackIndicator = null;
        }
    }

    @Inject(
            method = "renderItemHotbar",
            at = @At("HEAD")
    )
    public void combatControl$beforeRenderHotBar(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (combatAbilities.attackCooldown) return;

        // functionality from GoldenAgeCombat
        if (attackIndicator == null) {
            var option = minecraft.options.attackIndicator();
            attackIndicator = option.get();
            option.set(AttackIndicatorStatus.OFF);
        }
    }

    @Inject(
            method = "renderItemHotbar",
            at = @At("TAIL")
    )
    public void combatControl$afterRenderHotBar(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        // functionality from GoldenAgeCombat
        if (attackIndicator != null) {
            minecraft.options.attackIndicator().set(attackIndicator);
            attackIndicator = null;
        }
    }

    // combatControl$modifyRegeneratingHeartIndex is taken from GoldenAgeCombat
    @ModifyVariable(
            method = "renderHearts",
            at = @At("HEAD"),
            ordinal = 5,
            argsOnly = true
    )
    private int combatControl$modifyRegeneratingHeartIndex(int regeneratingHeartIndex) {
        if (CombatControlClient.get().config().isNoFlashingHearts()) {
            return 0;
        }

        return regeneratingHeartIndex;
    }
}
