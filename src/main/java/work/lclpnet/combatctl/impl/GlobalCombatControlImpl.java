package work.lclpnet.combatctl.impl;

import org.jetbrains.annotations.ApiStatus;
import work.lclpnet.combatctl.api.GlobalCombatControl;
import work.lclpnet.combatctl.config.ConfigAccess;
import work.lclpnet.combatctl.config.GlobalConfig;

import java.util.Objects;

@ApiStatus.Internal
public class GlobalCombatControlImpl implements GlobalCombatControl {

    private GlobalConfig globalConfig;

    public GlobalCombatControlImpl() {
        this.globalConfig = new GlobalConfig();
    }

    @Override
    public GlobalConfig getGlobalConfig() {
        return globalConfig;
    }

    public void bind(ConfigAccess access) {
        globalConfig = Objects.requireNonNull(access.config().global);
    }

    public static GlobalCombatControlImpl get() {
        return Holder.instance;
    }

    private static class Holder {
        private static final GlobalCombatControlImpl instance = new GlobalCombatControlImpl();
    }
}
