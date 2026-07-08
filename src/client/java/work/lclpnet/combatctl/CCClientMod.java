package work.lclpnet.combatctl;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import work.lclpnet.combatctl.impl.ClientStaticContext;
import work.lclpnet.combatctl.impl.CombatControlClientImpl;
import work.lclpnet.combatctl.impl.GlobalConfigContext;
import work.lclpnet.combatctl.impl.StaticCombatControl;
import work.lclpnet.combatctl.network.CombatAbilities;
import work.lclpnet.combatctl.network.CombatControlClientNetworking;
import work.lclpnet.kibu.config.ConfigManager;

public class CCClientMod implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		var configManager = CCModInit.configManager()
				.orElseThrow(() -> new IllegalStateException("combat-control config is not loaded"));

		var control = CombatControlClientImpl.get();
		control.bind(configManager);

		var networking = new CombatControlClientNetworking(control, CCModInit.LOGGER);
		networking.init();

		ClientLifecycleEvents.CLIENT_STOPPING.register(_ ->
				CCModInit.configManager().ifPresent(ConfigManager::close));

		ClientPlayConnectionEvents.JOIN.register((_, _, _) -> {
			// change static context to be controlled by the server
			ClientStaticContext ctx = new ClientStaticContext();
			StaticCombatControl.get().bind(ctx);

			control.bindClientContext(ctx);

			// reset combat abilities when joining another server
			control.abilities().copy(new CombatAbilities());
		});

		ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> {
			// reset context to local config when leaving a server
			GlobalConfigContext ctx = new GlobalConfigContext(configManager.config());

			control.bindClientContext(null);

			StaticCombatControl.get().bind(ctx);
		});
	}
}