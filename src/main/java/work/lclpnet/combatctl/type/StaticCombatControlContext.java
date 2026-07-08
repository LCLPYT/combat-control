package work.lclpnet.combatctl.type;

import org.jetbrains.annotations.NotNull;
import work.lclpnet.combatctl.config.GlobalConfig;

public interface StaticCombatControlContext {

    @NotNull GlobalConfig globalConfig();
}
