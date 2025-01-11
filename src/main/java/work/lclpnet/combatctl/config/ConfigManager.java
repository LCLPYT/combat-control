package work.lclpnet.combatctl.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.electronwill.nightconfig.core.serde.ObjectSerializer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

@ApiStatus.Internal
public class ConfigManager<C> implements ConfigAccess<C>, AutoCloseable {

    private final CommentedFileConfig fileConfig;
    private final ObjectSerializer serializer;
    private final ObjectDeserializer deserializer;
    private final C config;
    private @Nullable Runnable onChanged = null;

    public ConfigManager(Path configPath, C config) {
        this.config = config;

        fileConfig = CommentedFileConfig.builder(configPath)
                .autoreload()
                .onAutoReload(this::updateConfig)
                .build();

        serializer = ObjectSerializer.standard();
        deserializer = ObjectDeserializer.standard();
    }

    @Override
    public @NotNull C config() {
        return config;
    }

    public synchronized void load() {
        fileConfig.load();
        updateConfig();

        if (fileConfig.isEmpty()) {
            save();
        }
    }

    @Override
    public synchronized void save() {
        updateFileConfig();
        fileConfig.save();
    }

    private synchronized void updateConfig() {
        deserializer.deserializeFields(fileConfig, config);

        if (onChanged != null) {
            onChanged.run();
        }
    }

    private void updateFileConfig() {
        serializer.serializeFields(config, fileConfig);
    }

    @Override
    public void close() {
        if (fileConfig != null) {
            fileConfig.close();
        }
    }

    public void onChanged(@Nullable Runnable action) {
        onChanged = action;
    }
}
