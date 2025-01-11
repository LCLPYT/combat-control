package work.lclpnet.combatctl.cmd;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.json.JSONObject;
import org.slf4j.Logger;
import work.lclpnet.combatctl.CCModInit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ModTranslations {

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

    public MutableText fallback(String key, Object... args) {
        String fallback = defaultTranslations.getOrDefault(key, null);

        if (fallback != null) {
            fallback = fallback.formatted(args);
        }

        return Text.translatableWithFallback(key, fallback, args);
    }
}
