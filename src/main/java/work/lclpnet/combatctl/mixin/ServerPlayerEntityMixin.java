package work.lclpnet.combatctl.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import work.lclpnet.combatctl.impl.CombatConfig;
import work.lclpnet.combatctl.type.CombatControlPlayer;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements CombatControlPlayer {

    @Unique
    private CombatConfig config = null;

    @Override
    public void combatControl$setConfig(CombatConfig config) {
        this.config = config;
    }

    @Override
    public CombatConfig combatControl$getConfig() {
        return config;
    }
}
