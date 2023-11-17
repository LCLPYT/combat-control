package work.lclpnet.combatctl.compat;

import net.minecraft.entity.player.PlayerEntity;
import work.lclpnet.kibu.hook.player.PlayerFoodHooks;

class KibuHungerCompat implements HungerCompat {

    @Override
    public boolean onHungerLevelChange(PlayerEntity player, int from, int to) {
        return PlayerFoodHooks.LEVEL_CHANGE.invoker().onChange(player, from, to);
    }

    @Override
    public boolean onSaturationChange(PlayerEntity player, float from, float to) {
        return PlayerFoodHooks.SATURATION_CHANGE.invoker().onChange(player, from, to);
    }

    @Override
    public boolean onExhaustionChange(PlayerEntity player, float from, float to) {
        return PlayerFoodHooks.EXHAUSTION_CHANGE.invoker().onChange(player, from, to);
    }
}
