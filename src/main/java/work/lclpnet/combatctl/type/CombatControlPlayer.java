package work.lclpnet.combatctl.type;

import org.jetbrains.annotations.Nullable;
import work.lclpnet.combatctl.impl.CombatDetailConfig;

public interface CombatControlPlayer {

    void combatControl$setConfig(CombatDetailConfig config);

    @Nullable
    CombatDetailConfig combatControl$getConfig();
}
