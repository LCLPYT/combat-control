package work.lclpnet.combatctl;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.entity.attribute.EntityAttributes;
import work.lclpnet.combatctl.api.CombatControlClient;
import work.lclpnet.combatctl.event.AttributeModifierTooltipCallback;
import work.lclpnet.combatctl.network.CombatAbilities;
import work.lclpnet.combatctl.network.CombatControlClientNetworking;

public class CombatControlClientMod implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		CombatControlClient control = CombatControlClient.get();

		new CombatControlClientNetworking(control).init();

		registerEvents(control);
	}

	private static void registerEvents(CombatControlClient control) {
		CombatAbilities abilities = control.getAbilities();

		AttributeModifierTooltipCallback.EVENT.register((stack, player, attribute, modifier) -> {
			if (abilities.attackCooldown) return true;

			return attribute != EntityAttributes.ATTACK_SPEED;
		});
	}
}