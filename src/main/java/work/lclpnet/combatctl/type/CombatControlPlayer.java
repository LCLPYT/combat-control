package work.lclpnet.combatctl.type;

import org.jetbrains.annotations.Nullable;
import work.lclpnet.combatctl.config.PlayerConfig;
import work.lclpnet.combatctl.network.CombatAbilities;

public interface CombatControlPlayer {

    void combatControl$setConfig(PlayerConfig config);

    @Nullable PlayerConfig combatControl$getConfig();

    void combatControl$setAbilities(CombatAbilities abilities);

    @Nullable CombatAbilities combatControl$getAbilities();
}
