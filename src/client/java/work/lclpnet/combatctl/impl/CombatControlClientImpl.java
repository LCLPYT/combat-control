package work.lclpnet.combatctl.impl;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.combatctl.api.CombatControlClient;
import work.lclpnet.combatctl.config.ClientConfig;
import work.lclpnet.combatctl.config.CombatControlConfig;
import work.lclpnet.combatctl.network.CombatAbilities;
import work.lclpnet.kibu.config.ConfigAccess;

import java.util.Objects;

@ApiStatus.Internal
public class CombatControlClientImpl implements CombatControlClient {

    private final CombatAbilities abilities = new CombatAbilities();
    private CombatControlConfig config = new CombatControlConfig();  // only clientConfig may be accessed, the rest is managed by the server side
    private @Nullable ClientStaticContext context = null;

    @Override
    public CombatAbilities abilities() {
        return abilities;
    }

    @Override
    public ClientConfig config() {
        return config.client;
    }

    @Override
    public @Nullable ClientStaticContext serverContext() {
        return context;
    }

    public void bind(ConfigAccess<CombatControlConfig> access) {
        config = Objects.requireNonNull(access.config());
    }

    public void bindClientContext(@Nullable ClientStaticContext ctx) {
        this.context = ctx;
    }

    public static CombatControlClientImpl get() {
        return Holder.instance;
    }

    private static class Holder {
        private static final CombatControlClientImpl instance = new CombatControlClientImpl();
    }
}
