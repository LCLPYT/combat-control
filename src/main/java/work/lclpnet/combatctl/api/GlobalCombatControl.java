package work.lclpnet.combatctl.api;

import work.lclpnet.combatctl.config.GlobalConfig;
import work.lclpnet.combatctl.impl.GlobalCombatControlImpl;

public interface GlobalCombatControl {

    GlobalConfig getGlobalConfig();

    static GlobalCombatControl get() {
        return GlobalCombatControlImpl.get();
    }
}
