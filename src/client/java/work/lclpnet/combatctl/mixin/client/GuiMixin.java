package work.lclpnet.combatctl.mixin.client;

import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    private AttackIndicatorStatus attackIndicator = null;

    @Inject(
            method = "extractCrosshair",
            at = @At("HEAD")
    )
    public void combatControl$beforeRenderCrossHair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (combatAbilities.attackCooldown) return;

        // functionality from GoldenAgeCombat
        if (attackIndicator == null) {
            var option = minecraft.options.attackIndicator();
            attackIndicator = option.get();
            option.set(AttackIndicatorStatus.OFF);
        }
    }

    @Inject(
            method = "extractCrosshair",
            at = @At("TAIL")
    )
    public void combatControl$afterRenderCrossHair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        // functionality from GoldenAgeCombat
        if (attackIndicator != null) {
            minecraft.options.attackIndicator().set(attackIndicator);
            attackIndicator = null;
        }
    }

    @Inject(
            method = "extractItemHotbar",
            at = @At("HEAD")
    )
    public void combatControl$beforeRenderHotBar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (combatAbilities.attackCooldown) return;

        // functionality from GoldenAgeCombat
        if (attackIndicator == null) {
            var option = minecraft.options.attackIndicator();
            attackIndicator = option.get();
            option.set(AttackIndicatorStatus.OFF);
        }
    }

    @Inject(
            method = "extractItemHotbar",
            at = @At("TAIL")
    )
    public void combatControl$afterRenderHotBar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        // functionality from GoldenAgeCombat
        if (attackIndicator != null) {
            minecraft.options.attackIndicator().set(attackIndicator);
            attackIndicator = null;
        }
    }

    // combatControl$modifyRegeneratingHeartIndex is taken from GoldenAgeCombat
    @ModifyVariable(
            method = "extractHearts",
            at = @At("HEAD"),
            argsOnly = true,
            name = "oldHealth"
    )
    private int combatControl$modifyRegeneratingHeartIndex(int oldHealth) {
        if (CombatControlClient.get().config().isNoFlashingHearts()) {
            return 0;
        }

        return oldHealth;
    }
}
