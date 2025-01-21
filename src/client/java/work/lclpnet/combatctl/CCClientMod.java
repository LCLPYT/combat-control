package work.lclpnet.combatctl;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import work.lclpnet.combatctl.impl.CombatControlClientImpl;
import work.lclpnet.combatctl.network.CombatControlClientNetworking;
import work.lclpnet.kibu.config.ConfigManager;

public class CCClientMod implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		var configManager = CCModInit.configManager()
				.orElseThrow(() -> new IllegalStateException("combat-control config is not loaded"));

		var control = CombatControlClientImpl.get();
		control.bind(configManager);

		new CombatControlClientNetworking(control, CCModInit.LOGGER).init();

		ClientLifecycleEvents.CLIENT_STOPPING.register(client
				-> CCModInit.configManager().ifPresent(ConfigManager::close));
	}
}