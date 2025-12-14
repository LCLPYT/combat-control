package work.lclpnet.combatctl.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.compat.CompatManager;
import work.lclpnet.combatctl.compat.HungerCompat;
import work.lclpnet.combatctl.config.PlayerConfig;

/**
 * @implNote Mixin copied from GoldenAgeCombat and adapted to yarn mappings
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    @Shadow
    private int foodLevel = 20;
    @Shadow
    private float saturationLevel;
    @Shadow
    private float exhaustionLevel;
    @Shadow
    private int tickTimer;

    @Unique
    private final HungerCompat hungerCompat = CompatManager.get().getHungerCompat();

    @Inject(
            method = "tick",
            at = @At("HEAD"),
            cancellable = true
    )
    public void combatControl$tick(ServerPlayer player, CallbackInfo callback) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(serverPlayer);

        if (config.isModernRegeneration()) return;

        Difficulty difficulty = player.level().getDifficulty();
        if (this.exhaustionLevel > 4.0F) {
            float newExhaustion = this.exhaustionLevel - 4.0F;

            if (!hungerCompat.onExhaustionChange(player, this.exhaustionLevel, newExhaustion)) {
                this.exhaustionLevel = newExhaustion;
            }

            if (this.saturationLevel > 0.0F) {
                float newSaturationLevel = Math.max(this.saturationLevel - 1.0F, 0.0F);

                if (!hungerCompat.onSaturationChange(player, this.saturationLevel, newSaturationLevel)) {
                    this.saturationLevel = newSaturationLevel;
                }
            } else if (difficulty != Difficulty.PEACEFUL) {
                int newFoodLevel = Math.max(this.foodLevel - 1, 0);

                if (!hungerCompat.onHungerLevelChange(player, this.foodLevel, newFoodLevel)) {
                    this.foodLevel = newFoodLevel;
                }
            }
        }
        ServerLevel world = player.level();
        boolean flag = world.getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION);
        if (flag && this.foodLevel >= 18 && player.isHurt()) {
            ++this.tickTimer;
            if (this.tickTimer >= 80) {
                player.heal(1.0F);
                this.addExhaustion(3.0F);
                this.tickTimer = 0;
            }
        } else if (this.foodLevel <= 0) {
            ++this.tickTimer;
            if (this.tickTimer >= 80) {
                if (player.getHealth() > 10.0F || difficulty == Difficulty.HARD || player.getHealth() > 1.0F && difficulty == Difficulty.NORMAL) {
                    player.hurtServer(world, player.damageSources().starve(), 1.0F);
                }
                this.tickTimer = 0;
            }
        } else {
            this.tickTimer = 0;
        }
        callback.cancel();
    }

    @Shadow
    public abstract void addExhaustion(float amount);
}
