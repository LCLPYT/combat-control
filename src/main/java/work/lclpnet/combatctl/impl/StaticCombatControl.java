package work.lclpnet.combatctl.impl;

import org.jetbrains.annotations.ApiStatus;
import work.lclpnet.combatctl.config.CombatControlConfig;
import work.lclpnet.combatctl.config.ConfigAccess;
import work.lclpnet.combatctl.config.GlobalConfig;

import java.util.Objects;

/**
 * A singleton featuring data that needs to be available outside a server context, but on the server side, i.e. for features that don't have a server context.
 */
@ApiStatus.Internal
public class StaticCombatControl {

    private CombatControlConfig config = new CombatControlConfig();

    public GlobalConfig globalConfig() {
        return config.global;
    }

    public void bind(ConfigAccess<CombatControlConfig> access) {
        config = Objects.requireNonNull(access.config());
    }

    public static StaticCombatControl get() {
        return Holder.instance;
    }

    private static class Holder {
        private static final StaticCombatControl instance = new StaticCombatControl();
    }
}
