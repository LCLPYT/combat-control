package work.lclpnet.combatctl.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import work.lclpnet.combatctl.impl.CombatControlImpl;
import work.lclpnet.combatctl.type.CombatControlServer;

import java.util.Objects;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements CombatControlServer {

    @Unique
    private CombatControlImpl combatControl = null;

    @Override
    public void combatControl$set(CombatControlImpl combatControl) {
        this.combatControl = combatControl;
    }

    @Override
    public CombatControlImpl combatControl$get() {
        return Objects.requireNonNull(combatControl, "Combat control is not initialized yet");
    }
}
