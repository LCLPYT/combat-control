package work.lclpnet.combatctl.type;

import org.jetbrains.annotations.Nullable;
import work.lclpnet.combatctl.config.CombatConfig;
import work.lclpnet.combatctl.network.CombatAbilities;

public interface CombatControlPlayer {

    void combatControl$setConfig(CombatConfig config);

    @Nullable CombatConfig combatControl$getConfig();

    void combatControl$setAbilities(CombatAbilities abilities);

    @Nullable CombatAbilities combatControl$getAbilities();
}
