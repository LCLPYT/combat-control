package work.lclpnet.combatctl;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.entity.attribute.EntityAttributes;
import work.lclpnet.combatctl.api.CombatControlClient;
import work.lclpnet.combatctl.config.ConfigManager;
import work.lclpnet.combatctl.event.AttributeModifierTooltipCallback;
import work.lclpnet.combatctl.network.CombatAbilities;
import work.lclpnet.combatctl.network.CombatControlClientNetworking;

public class CCClientMod implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		CombatControlClient control = CombatControlClient.get();

		new CombatControlClientNetworking(control).init();

		CombatAbilities abilities = control.getAbilities();

		AttributeModifierTooltipCallback.EVENT.register((stack, player, attribute, modifier) -> {
			if (abilities.attackCooldown) return true;

			return attribute != EntityAttributes.ATTACK_SPEED;
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client
				-> CombatControlMod.configManager().ifPresent(ConfigManager::close));
	}
}