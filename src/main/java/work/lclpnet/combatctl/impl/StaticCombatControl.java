package work.lclpnet.combatctl.impl;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.combatctl.config.CombatControlConfig;
import work.lclpnet.combatctl.config.GlobalConfig;
import work.lclpnet.combatctl.type.StaticCombatControlContext;
import work.lclpnet.kibu.config.ConfigAccess;

import java.util.Objects;

/**
 * A singleton featuring data that needs to be available outside a server context, but on the server side, i.e. for features that don't have a server context.
 */
@ApiStatus.Internal
public class StaticCombatControl {

    private StaticCombatControlContext currentContext = new GlobalConfigContext(new CombatControlConfig());

    public synchronized @NotNull StaticCombatControlContext getContext() {
        return Objects.requireNonNull(currentContext);
    }

    public synchronized void bind(@NotNull StaticCombatControlContext context) {
        currentContext = Objects.requireNonNull(context);
    }

    public static StaticCombatControl get() {
        return Holder.instance;
    }

    private static class Holder {
        private static final StaticCombatControl instance = new StaticCombatControl();
    }
}
