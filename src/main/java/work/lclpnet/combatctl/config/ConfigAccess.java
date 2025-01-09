package work.lclpnet.combatctl.config;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ConfigAccess {

    CombatControlConfig config();

    void save();
}
