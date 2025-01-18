package work.lclpnet.combatctl.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.serde.ObjectDeserializer;
import com.electronwill.nightconfig.core.serde.ObjectSerializer;
import com.electronwill.nightconfig.core.serde.annotations.SerdeComment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApiStatus.Internal
public class ConfigManager<C> implements ConfigAccess<C>, AutoCloseable {

    private final CommentedFileConfig fileConfig;
    private final ObjectSerializer serializer;
    private final ObjectDeserializer deserializer;
    private final C config;
    private @Nullable Runnable onChanged = null;

    public ConfigManager(Path configPath, C config) {
        this.config = config;

        Path dir = configPath.getParent();

        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create config directory", e);
            }
        }

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
        var defaults = CommentedConfig.inMemory();
        updateDelegateConfig(defaults);

        fileConfig.load();

        boolean changed = addMissingEntries(defaults, fileConfig);

        updateConfig();

        if (changed) {
            fileConfig.save();
        }
    }

    @Override
    public synchronized void save() {
        updateDelegateConfig(fileConfig);
        fileConfig.save();
    }

    private synchronized void updateConfig() {
        deserializer.deserializeFields(fileConfig, config);

        if (onChanged != null) {
            onChanged.run();
        }
    }

    private void updateDelegateConfig(CommentedConfig cfg) {
        serializer.serializeFields(config, cfg);
        addEnumComments(cfg, config.getClass());
    }

    private boolean addMissingEntries(UnmodifiableCommentedConfig src, CommentedConfig dest) {
        boolean changed = false;

        for (var entry : src.entrySet()) {
            String key = entry.getKey();
            Object srcValue = entry.getValue();
            Object destValue = dest.getRaw(key);

            if (destValue == null) {
                dest.set(key, srcValue);
                dest.setComment(key, src.getComment(key));
                changed = true;
                continue;
            }

            if (srcValue instanceof UnmodifiableCommentedConfig nestedSrc && destValue instanceof CommentedConfig nestedDest) {
                changed |= addMissingEntries(nestedSrc, nestedDest);
            }
        }

        return changed;
    }

    private void addEnumComments(CommentedConfig cfg, Class<?> type) {
        for (var inst : ConfigOption.instanceTree(type)) {
            var field = inst.option().field();
            var fieldType = field.getType();

            if (!fieldType.isEnum()) continue;

            String path = Stream.concat(inst.srcPath().stream(), Stream.of(field))
                    .map(Field::getName)
                    .collect(Collectors.joining("."));

            if (!cfg.contains(path)) continue;

            StringBuilder longComment = new StringBuilder();
            String comment = cfg.getComment(path);

            if (comment != null) {
                longComment.append(comment);
                longComment.append('\n');
            }

            longComment.append("Possible options:");

            for (Field enumOpt : fieldType.getFields()) {
                longComment.append("\n    ");
                longComment.append(enumOpt.getName());

                String optComment = comment(enumOpt);

                if (optComment != null) {
                    longComment.append(": ");
                    longComment.append(optComment);
                }
            }

            cfg.setComment(path, longComment.toString());
        }
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

    public static @Nullable String comment(Field field) {
        SerdeComment[] comments = field.getDeclaredAnnotationsByType(SerdeComment.class);

        if (comments.length == 0) return null;

        var comment = new StringBuilder(comments[0].value());

        for (int i = 1; i < comments.length; i++) {
            comment.append("\n").append(comments[i].value());
        }

        return comment.toString();
    }
}
