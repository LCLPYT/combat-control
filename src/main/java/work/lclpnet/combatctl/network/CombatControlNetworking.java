package work.lclpnet.combatctl.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import work.lclpnet.combatctl.CCModInit;
import work.lclpnet.combatctl.network.packet.CombatAbilitiesS2CPacket;
import work.lclpnet.kibu.networking.protocol.Protocol;
import work.lclpnet.kibu.networking.protocol.ServerProtocolHandler;

public class CombatControlNetworking {

    public static final Protocol PROTOCOL = new Protocol(CCModInit.identifier("version"), 2);
    private final Logger logger;
    private @Nullable ServerProtocolHandler protocolHandler = null;

    public CombatControlNetworking(Logger logger) {
        this.logger = logger;
    }

    public void init() {
        protocolHandler = new ServerProtocolHandler(PROTOCOL, logger);
        protocolHandler.register();

        PayloadTypeRegistry.playS2C().register(CombatAbilitiesS2CPacket.ID, CombatAbilitiesS2CPacket.CODEC);
    }

    public boolean understands(ServerPlayerEntity player) {
        return protocolHandler != null && protocolHandler.understands(player);
    }
}
