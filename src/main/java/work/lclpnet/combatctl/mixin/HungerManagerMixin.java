package work.lclpnet.combatctl.mixin;

import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.impl.CombatConfig;

/**
 * @implNote Mixin copied from GoldenAgeCombat and adapted to yarn mappings
 */
@Mixin(HungerManager.class)
public abstract class HungerManagerMixin {

    @Shadow
    private int foodLevel = 20;
    @Shadow
    private float saturationLevel;
    @Shadow
    private float exhaustion;
    @Shadow
    private int foodTickTimer;
    @Shadow
    private int prevFoodLevel = 20;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    public void tick(PlayerEntity player, CallbackInfo callback) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(serverPlayer);

        if (config.isModernRegeneration()) return;

        Difficulty difficulty = player.getWorld().getDifficulty();
        this.prevFoodLevel = this.foodLevel;
        if (this.exhaustion > 4.0F) {
            this.exhaustion -= 4.0F;
            if (this.saturationLevel > 0.0F) {
                this.saturationLevel = Math.max(this.saturationLevel - 1.0F, 0.0F);
            } else if (difficulty != Difficulty.PEACEFUL) {
                this.foodLevel = Math.max(this.foodLevel - 1, 0);
            }
        }
        boolean flag = player.getWorld().getGameRules().getBoolean(GameRules.NATURAL_REGENERATION);
        if (flag && this.foodLevel >= 18 && player.canFoodHeal()) {
            ++this.foodTickTimer;
            if (this.foodTickTimer >= 80) {
                player.heal(1.0F);
                this.addExhaustion(3.0F);
                this.foodTickTimer = 0;
            }
        } else if (this.foodLevel <= 0) {
            ++this.foodTickTimer;
            if (this.foodTickTimer >= 80) {
                if (player.getHealth() > 10.0F || difficulty == Difficulty.HARD || player.getHealth() > 1.0F && difficulty == Difficulty.NORMAL) {
                    player.damage(player.getDamageSources().starve(), 1.0F);
                }
                this.foodTickTimer = 0;
            }
        } else {
            this.foodTickTimer = 0;
        }
        callback.cancel();
    }

    @Shadow
    public abstract void addExhaustion(float amount);
}
