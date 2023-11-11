package work.lclpnet.combatctl.config;

import org.slf4j.Logger;
import work.lclpnet.config.json.ConfigHandler;
import work.lclpnet.config.json.FileConfigSerializer;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ConfigManager implements ConfigAccess {

    private final ConfigHandler<CombatControlConfig> handler;

    public ConfigManager(Path configPath, Logger logger) {
        var serializer = new FileConfigSerializer<>(CombatControlConfig.FACTORY, logger);

        handler = new ConfigHandler<>(configPath, serializer, logger);
    }

    @Override
    public CombatControlConfig getConfig() {
        return handler.getConfig();
    }

    public CompletableFuture<Void> init() {
        return CompletableFuture.runAsync(handler::loadConfig);
    }
}
