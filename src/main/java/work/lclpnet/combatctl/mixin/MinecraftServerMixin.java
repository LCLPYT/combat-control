package work.lclpnet.combatctl.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.type.CombatControlServer;

import java.util.Objects;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements CombatControlServer {

    @Unique
    private CombatControl combatControl = null;

    @Override
    public void combatControl$set(CombatControl combatControl) {
        this.combatControl = combatControl;
    }

    @Override
    public CombatControl combatControl$get() {
        return Objects.requireNonNull(combatControl, "Combat control is not initialized yet");
    }
}
