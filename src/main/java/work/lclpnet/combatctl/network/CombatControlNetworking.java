package work.lclpnet.combatctl.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import work.lclpnet.combatctl.network.packet.CombatAbilitiesS2CPacket;

public class CombatControlNetworking {

    public static void init() {
        PayloadTypeRegistry.playS2C().register(CombatAbilitiesS2CPacket.ID, CombatAbilitiesS2CPacket.CODEC);
    }

    public static boolean isListening(ServerPlayerEntity player) {
        return ServerPlayNetworking.canSend(player, CombatAbilitiesS2CPacket.ID);
    }
}
