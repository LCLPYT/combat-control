package work.lclpnet.combatctl.impl;

import org.jspecify.annotations.NonNull;
import work.lclpnet.combatctl.config.GlobalConfig;
import work.lclpnet.combatctl.type.StaticCombatControlContext;

public class ClientStaticContext implements StaticCombatControlContext {

    private final GlobalConfig globalConfig = new GlobalConfig();

    @Override
    public @NonNull GlobalConfig globalConfig() {
        return globalConfig;
    }
}
