package work.lclpnet.combatctl.type;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.combatctl.impl.CombatControlImpl;
import work.lclpnet.combatctl.impl.KnockbackHandler;
import work.lclpnet.combatctl.impl.PingHandler;

@ApiStatus.Internal
public interface CombatControlServer {

    void combatControl$set(CombatControlImpl combatControl);

    CombatControlImpl combatControl$get();

    @NotNull PingHandler combatControl$getPingHandler();

    @NotNull KnockbackHandler combatControl$getKnockbackHandler();
}
