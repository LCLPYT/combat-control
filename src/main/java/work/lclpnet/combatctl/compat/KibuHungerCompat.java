package work.lclpnet.combatctl.compat;

import net.minecraft.world.entity.player.Player;
import work.lclpnet.kibu.hook.player.PlayerFoodHooks;

class KibuHungerCompat implements HungerCompat {

    @Override
    public boolean onHungerLevelChange(Player player, int from, int to) {
        return PlayerFoodHooks.LEVEL_CHANGE.invoker().onChange(player, from, to);
    }

    @Override
    public boolean onSaturationChange(Player player, float from, float to) {
        return PlayerFoodHooks.SATURATION_CHANGE.invoker().onChange(player, from, to);
    }

    @Override
    public boolean onExhaustionChange(Player player, float from, float to) {
        return PlayerFoodHooks.EXHAUSTION_CHANGE.invoker().onChange(player, from, to);
    }
}
