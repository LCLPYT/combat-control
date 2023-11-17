package work.lclpnet.combatctl.compat;

import net.minecraft.entity.player.PlayerEntity;

class VanillaHungerCompat implements HungerCompat {

    @Override
    public boolean onHungerLevelChange(PlayerEntity player, int from, int to) {
        return false;
    }

    @Override
    public boolean onSaturationChange(PlayerEntity player, float from, float to) {
        return false;
    }

    @Override
    public boolean onExhaustionChange(PlayerEntity player, float from, float to) {
        return false;
    }
}
