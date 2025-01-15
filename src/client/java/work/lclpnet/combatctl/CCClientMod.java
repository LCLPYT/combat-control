package work.lclpnet.combatctl;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.entity.attribute.EntityAttributes;
import work.lclpnet.combatctl.config.ConfigManager;
import work.lclpnet.combatctl.event.AttributeModifierTooltipCallback;
import work.lclpnet.combatctl.impl.CombatControlClientImpl;
import work.lclpnet.combatctl.network.CombatAbilities;
import work.lclpnet.combatctl.network.CombatControlClientNetworking;

public class CCClientMod implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		var configManager = CCModInit.configManager()
				.orElseThrow(() -> new IllegalStateException("combat-control config is not loaded"));

		var control = CombatControlClientImpl.get();
		control.bind(configManager);

		new CombatControlClientNetworking(control, CCModInit.LOGGER).init();

		CombatAbilities abilities = control.abilities();

		AttributeModifierTooltipCallback.EVENT.register((stack, player, attribute, modifier) -> {
			if (abilities.attackCooldown) return true;

			return attribute != EntityAttributes.ATTACK_SPEED;
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client
				-> CCModInit.configManager().ifPresent(ConfigManager::close));
	}
}