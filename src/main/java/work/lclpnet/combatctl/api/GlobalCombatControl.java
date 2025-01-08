package work.lclpnet.combatctl.api;

import work.lclpnet.combatctl.config.CombatGlobalConfig;
import work.lclpnet.combatctl.impl.GlobalCombatControlImpl;

public interface GlobalCombatControl {

    CombatGlobalConfig getGlobalConfig();

    static GlobalCombatControl get() {
        return GlobalCombatControlImpl.get();
    }
}
