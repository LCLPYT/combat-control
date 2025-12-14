package work.lclpnet.combatctl.cmd;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.json.JSONObject;
import org.slf4j.Logger;
import work.lclpnet.combatctl.CCModInit;
import work.lclpnet.combatctl.config.ConfigOption;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.join;
import static work.lclpnet.combatctl.CCModInit.MOD_ID;

public class ModTranslations {

    public static final String
            TITLE = MOD_ID + ".config.title",
            DESC = MOD_ID + ".config.desc",
            ENUM = MOD_ID + ".config.enum",
            ENUM_DESC = MOD_ID + ".config.enum_desc";

    private final Logger logger;
    private final Map<String, String> defaultTranslations = new HashMap<>();

    public ModTranslations(Logger logger) {
        this.logger = logger;
    }

    public CompletableFuture<Void> load() {
        return CompletableFuture.runAsync(this::_load);
    }

    private void _load() {
        String res = "/assets/%s/lang/en_us.json".formatted(CCModInit.MOD_ID);

        var in = getClass().getResourceAsStream(res);

        if (in == null) {
            logger.error("Failed to load default translations: Failed to get resource {}", res);
            return;
        }

        String jsonRaw;

        try (in) {
            jsonRaw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to read default translations", e);
            return;
        }

        var json = new JSONObject(jsonRaw);

        for (String key : json.keySet()) {
            String translation = json.optString(key);

            if (translation != null) {
                defaultTranslations.put(key, translation);
            }
        }
    }

    public MutableComponent fallback(String key, Object... args) {
        String fallback = defaultTranslations.getOrDefault(key, null);

        return Component.translatableWithFallback(key, fallback, args);
    }

    public MutableComponent enumName(Enum<?> enumVal, ConfigOption.Instance inst) {
        return fallback(enumNameKey(enumVal, inst.path()));
    }

    public MutableComponent optionTitle(ConfigOption.Instance inst) {
        return fallback(optionTitleKey(inst.path()));
    }

    public static String optionTitleKey(String path) {
        return join(".", TITLE, path);
    }

    public static String optionDescKey(String path) {
        return join(".", DESC, path);
    }

    public static String enumNameKey(Enum<?> enumVal, String path) {
        return join(".", ENUM, path, enumVal.name().toLowerCase(Locale.ROOT));
    }

    public static String enumDescKey(Enum<?> enumVal, String path) {
        return join(".", ENUM_DESC, path, enumVal.name().toLowerCase(Locale.ROOT));
    }
}
