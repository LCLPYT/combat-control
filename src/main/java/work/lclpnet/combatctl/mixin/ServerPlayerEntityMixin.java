package work.lclpnet.combatctl.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import work.lclpnet.combatctl.impl.CombatDetailConfig;
import work.lclpnet.combatctl.type.CombatControlPlayer;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements CombatControlPlayer {

    @Unique
    private CombatDetailConfig config = null;

    @Override
    public void combatControl$setConfig(CombatDetailConfig config) {
        this.config = config;
    }

    @Override
    public CombatDetailConfig combatControl$getConfig() {
        return config;
    }
}
