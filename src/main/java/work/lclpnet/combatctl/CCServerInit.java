package work.lclpnet.combatctl;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import work.lclpnet.combatctl.config.ConfigManager;

public class CCServerInit implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STOPPING.register(server
                -> CombatControlMod.configManager().ifPresent(ConfigManager::close));
    }
}
