package work.lclpnet.combatctl.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
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
        var defaults = Config.inMemory();
        serializer.serializeFields(config, defaults);

        fileConfig.load();

        boolean changed = addMissingEntries(defaults, fileConfig);

        updateConfig();

        if (changed) {
            fileConfig.save();
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

    private boolean addMissingEntries(UnmodifiableConfig src, Config dest) {
        boolean changed = false;

        for (var entry : src.entrySet()) {
            String key = entry.getKey();
            Object srcValue = entry.getValue();
            Object destValue = dest.getRaw(key);

            if (destValue == null) {
                dest.set(key, srcValue);
                changed = true;
                continue;
            }

            if (srcValue instanceof UnmodifiableConfig nestedSrc && destValue instanceof Config nestedDest) {
                changed |= addMissingEntries(nestedSrc, nestedDest);
            }
        }

        return changed;
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
