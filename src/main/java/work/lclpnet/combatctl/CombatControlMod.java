package work.lclpnet.combatctl;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.config.ConfigManager;
import work.lclpnet.combatctl.impl.CombatControlImpl;
import work.lclpnet.combatctl.type.CombatControlServer;

import java.nio.file.Path;

public class CombatControlMod implements ModInitializer {

	public static final String MOD_ID = "combat-control";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ConfigManager configManager = loadConfig();

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			CombatControl control = new CombatControlImpl(server, configManager);
			((CombatControlServer) server).combatControl$set(control);
		});

		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			CombatControl control = CombatControl.get(newPlayer.getServer());
			control.copyData(oldPlayer, newPlayer);
		});

		LOGGER.info("Initialized.");
	}

	private ConfigManager loadConfig() {
		Path configPath = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("config.json");
		ConfigManager configManager = new ConfigManager(configPath, LOGGER);

		configManager.init().join();

		return configManager;
	}

	/**
	 * Creates an identifier namespaced with the identifier of the mod.
	 * @param path The path.
	 * @return An identifier of this mod with the given path.
	 */
	public static Identifier identifier(String path) {
		return new Identifier(MOD_ID, path);
	}
}