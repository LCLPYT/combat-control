package work.lclpnet.combatctl;

import net.fabricmc.api.ClientModInitializer;
import work.lclpnet.combatctl.api.CombatControlClient;
import work.lclpnet.combatctl.network.CombatControlClientNetworking;

public class CombatControlClientMod implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		CombatControlClient control = CombatControlClient.getInstance();

		new CombatControlClientNetworking(control).init();
	}
}