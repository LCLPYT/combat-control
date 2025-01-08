package work.lclpnet.combatctl.api;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import work.lclpnet.combatctl.config.PlayerConfig;
import work.lclpnet.combatctl.type.CombatControlServer;

import java.util.function.Consumer;

public interface CombatControl {

    void setStyle(CombatStyle style);

    PlayerConfig getConfig(ServerPlayerEntity player);

    void copyData(ServerPlayerEntity source, ServerPlayerEntity target);

    void configure(ServerPlayerEntity player, Consumer<PlayerConfig> action);

    void update(ServerPlayerEntity player);

    default void setStyle(ServerPlayerEntity player, CombatStyle style) {
        configure(player, style::configure);
    }

    static CombatControl get(MinecraftServer server) {
        return ((CombatControlServer) server).combatControl$get();
    }
}