package work.lclpnet.combatctl.type;

import org.jetbrains.annotations.Nullable;
import work.lclpnet.combatctl.impl.CombatConfig;

public interface CombatControlPlayer {

    void combatControl$setConfig(CombatConfig config);

    @Nullable
    CombatConfig combatControl$getConfig();
}
