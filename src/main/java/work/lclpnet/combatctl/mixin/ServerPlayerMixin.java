package work.lclpnet.combatctl.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import work.lclpnet.combatctl.config.PlayerConfig;
import work.lclpnet.combatctl.network.CombatAbilities;
import work.lclpnet.combatctl.type.CombatControlPlayer;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin implements CombatControlPlayer {

    @Unique private volatile PlayerConfig config = null;
    @Unique private volatile CombatAbilities abilities = null;

    @Override
    public void combatControl$setConfig(PlayerConfig config) {
        this.config = config;
    }

    @Override
    public PlayerConfig combatControl$getConfig() {
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
