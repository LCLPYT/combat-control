package work.lclpnet.combatctl;

import net.fabricmc.api.ModInitializer;

public class CombatControlTest implements ModInitializer {

    @Override
    public void onInitialize() {
        CombatControlMod.LOGGER.info("Test mod loaded");
    }
}
