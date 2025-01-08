package work.lclpnet.combatctl.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.electronwill.nightconfig.core.serde.ObjectSerializer;

import java.nio.file.Path;

public class ConfigManager implements ConfigAccess, AutoCloseable {

    private final CommentedFileConfig config;
    private final ObjectSerializer serializer;
    private final ObjectDeserializer deserializer;
    private final CombatControlConfig obj = new CombatControlConfig();

    public ConfigManager(Path configPath) {
        config = CommentedFileConfig.builder(configPath)
                .autoreload()
                .onAutoReload(this::updateObj)
                .build();

        serializer = ObjectSerializer.standard();
        deserializer = ObjectDeserializer.standard();
    }

    @Override
    public CombatControlConfig config() {
        return obj;
    }

    public synchronized void load() {
        config.load();
        updateObj();

        if (config.isEmpty()) {
            save();
        }
    }

    public synchronized void save() {
        updateConfig();
        config.save();
    }

    private synchronized void updateObj() {
        deserializer.deserializeFields(config, obj);
    }

    private void updateConfig() {
        serializer.serializeFields(obj, config);
    }

    @Override
    public void close() {
        if (config != null) {
            config.close();
        }
    }
}
