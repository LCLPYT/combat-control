package work.lclpnet.combatctl.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import work.lclpnet.combatctl.config.CombatConfig;
import work.lclpnet.combatctl.network.CombatAbilities;
import work.lclpnet.combatctl.type.CombatControlPlayer;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements CombatControlPlayer {

    @Unique private volatile CombatConfig config = null;
    @Unique private volatile CombatAbilities abilities = null;

    @Override
    public void combatControl$setConfig(CombatConfig config) {
        this.config = config;
    }

    @Override
    public CombatConfig combatControl$getConfig() {
        return config;
    }

    @Override
    public synchronized void combatControl$setAbilities(CombatAbilities abilities) {
        this.abilities = abilities;
    }

    @Override
    public synchronized @Nullable CombatAbilities combatControl$getAbilities() {
        return abilities;
    }
}
