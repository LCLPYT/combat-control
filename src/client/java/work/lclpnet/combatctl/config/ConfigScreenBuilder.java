package work.lclpnet.combatctl.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.kibu.config.ConfigManager;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Consumer;

import static java.lang.String.join;
import static net.minecraft.text.Text.translatable;
import static net.minecraft.text.Text.translatableWithFallback;
import static work.lclpnet.combatctl.cmd.ModTranslations.*;

public class ConfigScreenBuilder implements ConfigScreenFactory<Screen> {

    private final ConfigManager<CombatControlConfig> configManager;
    private final CombatControlConfig config, defaultConfig;

    public ConfigScreenBuilder(ConfigManager<CombatControlConfig> configManager) {
        this.configManager = configManager;
        this.config = configManager.config();
        this.defaultConfig = new CombatControlConfig();
    }

    private void save() {
        MinecraftClient client = MinecraftClient.getInstance();
        IntegratedServer server = client.getServer();

        if (server != null) {
            var control = CombatControl.get(server);
            PlayerLookup.all(server).forEach(control::resetPlayerConfig);
            control.update();
        } else {
            configManager.save();
        }
    }

    @Override
    public Screen create(Screen parent) {
        var builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(translatable(TITLE))
                .setSavingRunnable(this::save);

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

        var category = builder.getOrCreateCategory(translatableWithFallback(optionTitleKey(name), name));

        String comment = ConfigManager.comment(field);

        if (comment != null) {
            category.setDescription(new StringVisitable[] {
                    translatableWithFallback(optionDescKey(name), comment)
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
            String comment = ConfigManager.comment(field);

            if (!ConfigOption.isValue(type)) {
                continue;
            }

            var option = new ConfigOption(field, srcClass);
            Object value = option.get(src);
            Object defaultValue = option.get(defaultSrc);

            if (value == null || defaultValue == null) continue;

            var label = translatable(optionTitleKey(join(".", parent.getName(), name)));
            String path = join(".", parent.getName(), name);

            var tooltip = comment != null
                    ? translatableWithFallback(optionDescKey(path), comment)
                    : null;

            var entry = entry(new EntryData(builder, type, value, defaultValue, v -> option.set(src, v), label, path, tooltip));

            if (entry != null) {
                category.addEntry(entry);
            }
        }
    }

    private @Nullable AbstractConfigListEntry<?> entry(EntryData data) {
        if (data.type == boolean.class) {
            return data.builder.entryBuilder()
                    .startBooleanToggle(data.label, data.value instanceof Boolean b && b)
                    .setDefaultValue(data.defaultValue instanceof Boolean b && b)
                    .setTooltip(data.tooltip)
                    .setSaveConsumer(data.saveConsumer::accept)
                    .build();
        }

        if (data.type == double.class) {
            double dd = data.defaultValue instanceof Number n ? n.doubleValue() : 0.d;

            return data.builder.entryBuilder()
                    .startDoubleField(data.label, data.value instanceof Number n ? n.doubleValue() : dd)
                    .setDefaultValue(dd)
                    .setTooltip(data.tooltip)
                    .setSaveConsumer(data.saveConsumer::accept)
                    .build();
        }

        if (data.type == int.class) {
            int di = data.defaultValue instanceof Number n ? n.intValue() : 0;

            return data.builder.entryBuilder()
                    .startIntField(data.label, data.value instanceof Number n ? n.intValue() : di)
                    .setDefaultValue(di)
                    .setTooltip(data.tooltip)
                    .setSaveConsumer(data.saveConsumer::accept)
                    .build();
        }

        if (data.type.isEnum()) {
            return enumSelector(data);
        }

        return null;
    }

    // convince the compiler that some class is an enum and that the value is an enum constant of it 💀💀💀
    @SuppressWarnings("unchecked")
    private <T extends Enum<T>> EnumListEntry<?> enumSelector(EntryData data) {
        return data.builder.entryBuilder()
                .startEnumSelector(data.label, (Class<T>) data.type(), (T) data.value)
                .setDefaultValue((T) data.defaultValue)
                .setTooltipSupplier(val -> {
                    Field field;

                    try {
                        field = data.type().getField(val.name());
                    } catch (NoSuchFieldException e) {
                        return Optional.ofNullable(data.tooltip).map(t -> new Text[] {t});
                    }

                    String comment = ConfigManager.comment(field);

                    if (comment == null) {
                        return Optional.ofNullable(data.tooltip).map(t -> new Text[] {t});
                    }

                    Text desc = data.enumName(val)
                            .append(": ")
                            .append(translatableWithFallback(enumDescKey(val, data.path), comment));

                    return data.tooltip == null
                            ? Optional.of(new Text[]{desc})
                            : Optional.of(new Text[]{data.tooltip, desc});
                })
                .setEnumNameProvider(data::enumName)
                .setSaveConsumer(data.saveConsumer::accept)
                .build();
    }

    private record EntryData(
            ConfigBuilder builder,
            Class<?> type, Object value,
            Object defaultValue,
            Consumer<Object> saveConsumer,
            Text label,
            String path,
            @Nullable Text tooltip) {

        public MutableText enumName(Enum<?> val) {
            return translatableWithFallback(enumNameKey(val, path), val.name());
        }
    }
}
