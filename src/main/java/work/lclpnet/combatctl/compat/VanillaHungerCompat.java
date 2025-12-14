package work.lclpnet.combatctl.compat;

import net.minecraft.world.entity.player.Player;

class VanillaHungerCompat implements HungerCompat {

    @Override
    public boolean onHungerLevelChange(Player player, int from, int to) {
        return false;
    }

    @Override
    public boolean onSaturationChange(Player player, float from, float to) {
        return false;
    }

    @Override
    public boolean onExhaustionChange(Player player, float from, float to) {
        return false;
    }
}
