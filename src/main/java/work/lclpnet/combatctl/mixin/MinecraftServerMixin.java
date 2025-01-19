package work.lclpnet.combatctl.mixin;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import work.lclpnet.combatctl.impl.CombatControlImpl;
import work.lclpnet.combatctl.impl.KnockbackHandler;
import work.lclpnet.combatctl.impl.PingHandler;
import work.lclpnet.combatctl.type.CombatControlServer;

import java.util.Objects;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements CombatControlServer {

    @Unique private final Object ccLock = new Object[0];
    @Unique private CombatControlImpl combatControl = null;
    @Unique private volatile PingHandler pingHandler = null;
    @Unique private volatile KnockbackHandler knockbackHandler = null;

    @Override
    public void combatControl$set(CombatControlImpl combatControl) {
        this.combatControl = combatControl;
    }

    @Override
    public CombatControlImpl combatControl$get() {
        return Objects.requireNonNull(combatControl, "Combat control is not initialized yet");
    }

    @Override
    public @NotNull PingHandler combatControl$getPingHandler() {
        if (pingHandler != null) {
            return pingHandler;
        }

        synchronized (ccLock) {
            if (pingHandler == null) {
                pingHandler = new PingHandler((MinecraftServer) (Object) this);
            }
        }

        return pingHandler;
    }

    @Override
    public @NotNull KnockbackHandler combatControl$getKnockbackHandler() {
        if (knockbackHandler != null) {
            return knockbackHandler;
        }

        synchronized (ccLock) {
            if (knockbackHandler == null) {
                knockbackHandler = new KnockbackHandler();
            }
        }

        return knockbackHandler;
    }
}
