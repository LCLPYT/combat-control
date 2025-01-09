package work.lclpnet.combatctl.api;

import work.lclpnet.combatctl.config.GlobalConfig;
import work.lclpnet.combatctl.config.PlayerConfig;

import java.util.function.Consumer;

public interface CombatStyle {

    void configure(PlayerConfig player);

    void configure(GlobalConfig global);

    default CombatStyle andThen(Consumer<PlayerConfig> playerOverride, Consumer<GlobalConfig> globalOverride) {
        return new CombatStyle() {
            @Override
            public void configure(PlayerConfig player) {
                CombatStyle.this.configure(player);
                playerOverride.accept(player);
            }

            @Override
            public void configure(GlobalConfig global) {
                CombatStyle.this.configure(global);
                globalOverride.accept(global);
            }
        };
    }
}
