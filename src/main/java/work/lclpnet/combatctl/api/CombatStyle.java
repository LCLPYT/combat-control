package work.lclpnet.combatctl.api;

import work.lclpnet.combatctl.config.CombatControlConfig;
import work.lclpnet.combatctl.config.GlobalConfig;
import work.lclpnet.combatctl.config.PlayerConfig;

public interface CombatStyle {

    void configure(PlayerConfig player);

    void configure(GlobalConfig global);

    default void configure(CombatControlConfig config) {
        configure(config.player);
        configure(config.global);
    }
}
