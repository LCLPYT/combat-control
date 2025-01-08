package work.lclpnet.combatctl.api;

import work.lclpnet.combatctl.config.CombatConfig;
import work.lclpnet.combatctl.config.CombatControlConfig;
import work.lclpnet.combatctl.config.CombatGlobalConfig;

public interface CombatStyle {

    void configure(CombatConfig player);

    void configure(CombatGlobalConfig global);

    default void configure(CombatControlConfig config) {
        configure(config.player);
        configure(config.global);
    }
}
