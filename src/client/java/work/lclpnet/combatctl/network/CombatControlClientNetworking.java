package work.lclpnet.combatctl.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;
import work.lclpnet.combatctl.api.CombatControlClient;
import work.lclpnet.combatctl.impl.ClientStaticContext;
import work.lclpnet.combatctl.network.packet.CombatAbilitiesS2CPacket;
import work.lclpnet.kibu.networking.protocol.ClientProtocolHandler;

public class CombatControlClientNetworking {

    private final CombatControlClient control;
    private final Logger logger;

    public CombatControlClientNetworking(CombatControlClient control, Logger logger) {
        this.control = control;
        this.logger = logger;
    }

    public void init() {
        new ClientProtocolHandler(CombatControlNetworking.PROTOCOL, logger).register();

        ClientPlayNetworking.registerGlobalReceiver(CombatAbilitiesS2CPacket.ID, this::onAbilitiesUpdate);
    }

    private void onAbilitiesUpdate(CombatAbilitiesS2CPacket payload, ClientPlayNetworking.Context context) {
        control.abilities().copy(payload.abilities());

        ClientStaticContext serverContext = control.serverContext();

        if (serverContext != null) {
            serverContext.globalConfig().setLargerHitboxes(payload.abilities().largerHitboxes);
        }
    }
}
