package work.lclpnet.combatctl.config;

import org.json.JSONObject;
import work.lclpnet.combatctl.api.CombatStyle;
import work.lclpnet.config.json.JsonConfig;
import work.lclpnet.config.json.JsonConfigFactory;

public class CombatControlConfig implements JsonConfig {

    public CombatStyle combatStyle = CombatStyle.MODERN;

    public CombatControlConfig() {}

    public CombatControlConfig(JSONObject json) {
        if (json.has("combat-style")) {
            CombatStyle style = CombatStyle.tryFrom(json.getString("combat-style"));

            if (style != null) this.combatStyle = style;
        }
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();

        json.put("combat-style", combatStyle.getValue());

        return json;
    }

    public static final JsonConfigFactory<CombatControlConfig> FACTORY = new JsonConfigFactory<>() {
        @Override
        public CombatControlConfig createDefaultConfig() {
            return new CombatControlConfig();
        }

        @Override
        public CombatControlConfig createConfig(JSONObject json) {
            return new CombatControlConfig(json);
        }
    };
}
