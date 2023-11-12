package work.lclpnet.combatctl.impl;

import org.jetbrains.annotations.ApiStatus;
import work.lclpnet.combatctl.api.CombatControlClient;
import work.lclpnet.combatctl.network.CombatAbilities;

@ApiStatus.Internal
public class CombatControlClientImpl implements CombatControlClient {

    private final CombatAbilities abilities = new CombatAbilities();

    @Override
    public CombatAbilities getAbilities() {
        return abilities;
    }

    public static CombatControlClientImpl get() {
        return Holder.instance;
    }

    private static class Holder {
        private static final CombatControlClientImpl instance = new CombatControlClientImpl();
    }
}
