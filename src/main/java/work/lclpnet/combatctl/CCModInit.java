package work.lclpnet.combatctl;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.cmd.CombatCommand;
import work.lclpnet.combatctl.cmd.ModTranslations;
import work.lclpnet.combatctl.config.CombatControlConfig;
import work.lclpnet.combatctl.config.ConfigManager;
import work.lclpnet.combatctl.impl.CombatControlImpl;
import work.lclpnet.combatctl.impl.StaticCombatControl;
import work.lclpnet.combatctl.impl.SwordBlockingHandler;
import work.lclpnet.combatctl.network.CombatControlNetworking;
import work.lclpnet.combatctl.type.CombatControlServer;

import java.nio.file.Path;
import java.util.Optional;

@ApiStatus.Internal
public class CCModInit implements ModInitializer {

	public static final String MOD_ID = "combat-control";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static volatile ConfigManager<CombatControlConfig> _configManager = null;

	@Override
	public void onInitialize() {
		var configManager = loadConfig();
		_configManager = configManager;
		StaticCombatControl.get().bind(configManager);

		var translations = new ModTranslations(LOGGER);
		translations.load().join();

		var networking = new CombatControlNetworking(LOGGER);
		networking.init();

		var swordBlockingHandler = new SwordBlockingHandler();

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			var control = new CombatControlImpl(server, configManager, networking);
			((CombatControlServer) server).combatControl$set(control);

			configManager.onChanged(control::updatePlayers);

			swordBlockingHandler.init();
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            configManager.onChanged(null);

			swordBlockingHandler.destroy();
        });

		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			var control = CombatControl.get(newPlayer.getServer());
			control.copyData(oldPlayer, newPlayer);
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			var control = CombatControl.get(server);

			ServerPlayerEntity player = handler.player;

			if (player != null) {
				control.update(player);
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
				-> new CombatCommand(translations, configManager).register(dispatcher));

		LOGGER.info("Initialized.");
	}

	private ConfigManager<CombatControlConfig> loadConfig() {
		Path configPath = FabricLoader.getInstance().getConfigDir()
				.resolve(MOD_ID)
				.resolve("config.toml");


		var configManager = new ConfigManager<>(configPath, new CombatControlConfig());

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

	public static String permission(String suffix) {
		return MOD_ID + "." + suffix;
	}

	public static Optional<ConfigManager<CombatControlConfig>> configManager() {
		return Optional.ofNullable(_configManager);
	}
}