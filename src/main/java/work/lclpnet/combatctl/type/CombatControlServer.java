package work.lclpnet.combatctl.type;

import org.jetbrains.annotations.ApiStatus;
import work.lclpnet.combatctl.impl.CombatControlImpl;

@ApiStatus.Internal
public interface CombatControlServer {

    void combatControl$set(CombatControlImpl combatControl);

    CombatControlImpl combatControl$get();
}
