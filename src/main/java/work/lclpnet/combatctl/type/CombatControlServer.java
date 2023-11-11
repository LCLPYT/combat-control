package work.lclpnet.combatctl.type;

import work.lclpnet.combatctl.api.CombatControl;

public interface CombatControlServer {

    void combatControl$set(CombatControl combatControl);

    CombatControl combatControl$get();
}
