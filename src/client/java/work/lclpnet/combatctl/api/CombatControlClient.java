package work.lclpnet.combatctl.api;

import work.lclpnet.combatctl.impl.CombatControlClientImpl;
import work.lclpnet.combatctl.network.CombatAbilities;

public interface CombatControlClient {

    CombatAbilities getAbilities();

    static CombatControlClient getInstance() {
        return CombatControlClientImpl.getInstance();
    }
}
