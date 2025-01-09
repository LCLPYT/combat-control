package work.lclpnet.combatctl;

import net.fabricmc.api.ModInitializer;

public class CombatControlTest implements ModInitializer {

    @Override
    public void onInitialize() {
        CCModInit.LOGGER.info("Test mod loaded");
    }
}
