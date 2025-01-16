package work.lclpnet.combatctl.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.render.RenderTickCounter;
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

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Shadow @Final private MinecraftClient client;

    @Unique
    private final CombatAbilities combatAbilities = CombatControlClient.get().abilities();
    @Unique
    @Nullable
    private static AttackIndicator attackIndicator = null;

    @Inject(
            method = "renderCrosshair",
            at = @At("HEAD")
    )
    public void combatControl$beforeRenderCrossHair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (combatAbilities.attackCooldown) return;

        // functionality from GoldenAgeCombat
        if (attackIndicator == null) {
            var option = client.options.getAttackIndicator();
            attackIndicator = option.getValue();
            option.setValue(AttackIndicator.OFF);
        }
    }

    @Inject(
            method = "renderCrosshair",
            at = @At("TAIL")
    )
    public void combatControl$afterRenderCrossHair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        // functionality from GoldenAgeCombat
        if (attackIndicator != null) {
            client.options.getAttackIndicator().setValue(attackIndicator);
            attackIndicator = null;
        }
    }

    @Inject(
            method = "renderHotbar",
            at = @At("HEAD")
    )
    public void combatControl$beforeRenderHotBar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (combatAbilities.attackCooldown) return;

        // functionality from GoldenAgeCombat
        if (attackIndicator == null) {
            var option = client.options.getAttackIndicator();
            attackIndicator = option.getValue();
            option.setValue(AttackIndicator.OFF);
        }
    }

    @Inject(
            method = "renderHotbar",
            at = @At("TAIL")
    )
    public void combatControl$afterRenderHotBar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        // functionality from GoldenAgeCombat
        if (attackIndicator != null) {
            client.options.getAttackIndicator().setValue(attackIndicator);
            attackIndicator = null;
        }
    }

    @ModifyVariable(
            method = "renderHealthBar",
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
