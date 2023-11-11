package work.lclpnet.combatctl.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import work.lclpnet.combatctl.network.packet.CombatAbilitiesS2CPacket;

public class CombatControlNetworking {

    public static boolean isListening(ServerPlayerEntity player) {
        return ServerPlayNetworking.canSend(player, CombatAbilitiesS2CPacket.TYPE);
    }

    public static void send(ServerPlayerEntity player, FabricPacket packet) {
        ServerPlayNetworking.send(player, packet);
    }
}
