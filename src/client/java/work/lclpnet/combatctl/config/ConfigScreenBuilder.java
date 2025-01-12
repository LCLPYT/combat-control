package work.lclpnet.combatctl.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeComment;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;

import static java.lang.String.join;
import static net.minecraft.text.Text.translatable;
import static net.minecraft.text.Text.translatableWithFallback;
import static work.lclpnet.combatctl.CCModInit.MOD_ID;

public class ConfigScreenBuilder implements ConfigScreenFactory<Screen> {

    private static final String
            TITLE = MOD_ID + ".config.title",
            DESC = MOD_ID + ".config.desc";

    private final ConfigManager<CombatControlConfig> configManager;
    private final CombatControlConfig config, defaultConfig;

    public ConfigScreenBuilder(ConfigManager<CombatControlConfig> configManager) {
        this.configManager = configManager;
        this.config = configManager.config();
        this.defaultConfig = new CombatControlConfig();
    }

    @Override
    public Screen create(Screen parent) {
        var builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(translatable(TITLE))
                .setSavingRunnable(configManager::save);

        addCategory("client", builder);

        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();

        // when not connected to a multiplayer server, show player and global
        if (networkHandler == null || networkHandler.getServerInfo() == null) {
            addCategory("player", builder);
            addCategory("global", builder);
        }

        return builder.build();
    }

    private void addCategory(String name, ConfigBuilder builder) {
        Field field;

        try {
            field = CombatControlConfig.class.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return;
        }

        var category = builder.getOrCreateCategory(translatable(join(".", TITLE, name)));

        String comment = comment(field);

        if (comment != null) {
            category.setDescription(new StringVisitable[] {
                    translatableWithFallback(join(".", DESC, name), comment)
            });
        }

        Object src, defaultSrc;

        try {
            field.setAccessible(true);
            src = field.get(config);
            defaultSrc = field.get(defaultConfig);
        } catch (IllegalAccessException ignored) {
            return;
        }

        initCategory(builder, category, field, src, defaultSrc);
    }

    private void initCategory(ConfigBuilder builder, ConfigCategory category, Field parent, Object src, Object defaultSrc) {
        var srcClass = parent.getType();

        for (Field field : srcClass.getDeclaredFields()) {
            var type = field.getType();
            String name = field.getName();
            String comment = comment(field);

            if (ConfigOption.isValue(type)) {
                var option = new ConfigOption(field, srcClass);
                Object value = option.get(src);
                Object defaultValue = option.get(defaultSrc);

                if (value == null || defaultValue == null) continue;

                var label = translatable(join(".", TITLE, parent.getName(), name));
                var tooltip = comment != null
                        ? translatableWithFallback(join(".", DESC, parent.getName(), name), comment)
                        : null;

                var entry = entry(builder, type, value, defaultValue, v -> option.set(src, v), label, tooltip);

                if (entry != null) {
                    category.addEntry(entry);
                }
            }
        }
    }

    private @Nullable AbstractConfigListEntry<?> entry(ConfigBuilder builder, Class<?> type,
                                                       Object value, Object defaultValue,
                                                       Consumer<Object> saveConsumer,
                                                       Text label, @Nullable Text tooltip) {
        if (type == boolean.class) {
            return builder.entryBuilder()
                    .startBooleanToggle(label, value instanceof Boolean b && b)
                    .setDefaultValue(defaultValue instanceof Boolean b && b)
                    .setTooltip(tooltip)
                    .setSaveConsumer(saveConsumer::accept)
                    .build();
        }

        return null;
    }

    private static @Nullable String comment(Field field) {
        SerdeComment[] comments = field.getDeclaredAnnotationsByType(SerdeComment.class);

        if (comments.length == 0) return null;

        var comment = new StringBuilder(comments[0].value());

        for (int i = 1; i < comments.length; i++) {
            comment.append("\n").append(comments[i].value());
        }

        return comment.toString();
    }
}
