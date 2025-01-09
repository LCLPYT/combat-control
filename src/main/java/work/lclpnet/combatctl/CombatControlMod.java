package work.lclpnet.combatctl;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.cmd.CombatCommand;
import work.lclpnet.combatctl.cmd.ModTranslations;
import work.lclpnet.combatctl.config.ConfigManager;
import work.lclpnet.combatctl.impl.CombatControlImpl;
import work.lclpnet.combatctl.impl.StaticCombatControl;
import work.lclpnet.combatctl.network.CombatControlNetworking;
import work.lclpnet.combatctl.type.CombatControlServer;

import java.nio.file.Path;
import java.util.Optional;

@ApiStatus.Internal
public class CombatControlMod implements ModInitializer {

	public static final String MOD_ID = "combat-control";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static volatile ConfigManager _configManager = null;

	@Override
	public void onInitialize() {
		ConfigManager configManager = loadConfig();
		_configManager = configManager;
		StaticCombatControl.get().bind(configManager);

		var translations = new ModTranslations(LOGGER);
		translations.load().join();

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			var control = new CombatControlImpl(server, configManager);
			((CombatControlServer) server).combatControl$set(control);
			configManager.onChanged(control::updatePlayers);
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server
				-> configManager.onChanged(null));

		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			CombatControl control = CombatControl.get(newPlayer.getServer());
			control.copyData(oldPlayer, newPlayer);
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
				-> new CombatCommand(translations).register(dispatcher));

		CombatControlNetworking.init();

		LOGGER.info("Initialized.");
	}

	private ConfigManager loadConfig() {
		Path configPath = FabricLoader.getInstance().getConfigDir()
				.resolve(MOD_ID)
				.resolve("config.toml");

		ConfigManager configManager = new ConfigManager(configPath);

		configManager.load();

		return configManager;
	}

	/**
	 * Creates an identifier namespaced with the identifier of the mod.
	 * @param path The path.
	 * @return An identifier of this mod with the given path.
	 */
	public static Identifier identifier(String path) {
		return Identifier.of(MOD_ID, path);
	}

	public static Optional<ConfigManager> configManager() {
		return Optional.ofNullable(_configManager);
	}
}