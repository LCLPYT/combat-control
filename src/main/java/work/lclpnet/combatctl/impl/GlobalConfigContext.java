package work.lclpnet.combatctl.impl;

import org.jspecify.annotations.NonNull;
import work.lclpnet.combatctl.config.CombatControlConfig;
import work.lclpnet.combatctl.config.GlobalConfig;
import work.lclpnet.combatctl.type.StaticCombatControlContext;

public class GlobalConfigContext implements StaticCombatControlContext {

    private final CombatControlConfig config;

    public GlobalConfigContext(CombatControlConfig config) {
        this.config = config;
    }

    @Override
    public @NonNull GlobalConfig globalConfig() {
        return config.global;
    }
}
