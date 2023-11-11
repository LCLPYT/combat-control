package work.lclpnet.combatctl.api;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import work.lclpnet.combatctl.impl.CombatConfig;
import work.lclpnet.combatctl.type.CombatControlServer;

public interface CombatControl {

    void setStyle(CombatStyle style);

    CombatStyle getStyle();

    void setStyle(ServerPlayerEntity player, CombatStyle style);

    CombatConfig getConfig(ServerPlayerEntity player);

    void copyData(ServerPlayerEntity source, ServerPlayerEntity target);

    static CombatControl get(MinecraftServer server) {
        return ((CombatControlServer) server).combatControl$get();
    }
}