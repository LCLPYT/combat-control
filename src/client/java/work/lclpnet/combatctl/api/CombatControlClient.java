package work.lclpnet.combatctl.api;

import org.jetbrains.annotations.Nullable;
import work.lclpnet.combatctl.config.ClientConfig;
import work.lclpnet.combatctl.impl.ClientStaticContext;
import work.lclpnet.combatctl.impl.CombatControlClientImpl;
import work.lclpnet.combatctl.network.CombatAbilities;

public interface CombatControlClient {

    CombatAbilities abilities();

    ClientConfig config();

    @Nullable ClientStaticContext serverContext();

    static CombatControlClient get() {
        return CombatControlClientImpl.get();
    }
}
