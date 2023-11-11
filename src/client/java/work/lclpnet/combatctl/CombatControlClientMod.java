package work.lclpnet.combatctl;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.entity.attribute.EntityAttributes;
import work.lclpnet.combatctl.api.CombatControlClient;
import work.lclpnet.combatctl.impl.AttributeTooltipHelper;
import work.lclpnet.combatctl.network.CombatAbilities;
import work.lclpnet.combatctl.network.CombatControlClientNetworking;

public class CombatControlClientMod implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		CombatControlClient control = CombatControlClient.getInstance();

		new CombatControlClientNetworking(control).init();

		registerEvents(control);
	}

	private static void registerEvents(CombatControlClient control) {
		CombatAbilities abilities = control.getAbilities();

		ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
			if (abilities.attackCooldown) return;

			lines.removeIf(text -> AttributeTooltipHelper.matchesAttributeComponent(text, EntityAttributes.GENERIC_ATTACK_SPEED, null));
		});
	}
}