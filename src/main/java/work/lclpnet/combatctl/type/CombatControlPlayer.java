package work.lclpnet.combatctl.type;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.combatctl.config.PlayerConfig;
import work.lclpnet.combatctl.network.CombatAbilities;

@ApiStatus.Internal
public interface CombatControlPlayer {

    void combatControl$setConfig(PlayerConfig config);

    @Nullable PlayerConfig combatControl$getConfig();

    void combatControl$setAbilities(CombatAbilities abilities);

    @Nullable CombatAbilities combatControl$getAbilities();
}
