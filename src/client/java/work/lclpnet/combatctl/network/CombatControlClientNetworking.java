package work.lclpnet.combatctl.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.network.ClientPlayerEntity;
import work.lclpnet.combatctl.api.CombatControlClient;
import work.lclpnet.combatctl.network.packet.CombatAbilitiesS2CPacket;

public class CombatControlClientNetworking {

    private final CombatControlClient control;

    public CombatControlClientNetworking(CombatControlClient control) {
        this.control = control;
    }

    public void init() {
        ClientPlayNetworking.registerGlobalReceiver(CombatAbilitiesS2CPacket.TYPE, this::onAbilitiesUpdate);
    }

    private void onAbilitiesUpdate(CombatAbilitiesS2CPacket packet, ClientPlayerEntity player, PacketSender responseSender) {
        control.getAbilities().copy(packet.getAbilities());
    }
}
