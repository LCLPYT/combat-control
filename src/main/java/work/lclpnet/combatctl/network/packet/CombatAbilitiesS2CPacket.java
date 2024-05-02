package work.lclpnet.combatctl.network.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import work.lclpnet.combatctl.CombatControlMod;
import work.lclpnet.combatctl.network.CombatAbilities;

public record CombatAbilitiesS2CPacket(CombatAbilities abilities) implements CustomPayload {

    public static final Id<CombatAbilitiesS2CPacket> ID = new Id<>(CombatControlMod.identifier("abilities"));

    public static final PacketCodec<PacketByteBuf, CombatAbilitiesS2CPacket> CODEC = PacketCodec.tuple(
            CombatAbilities.PACKET_CODEC, CombatAbilitiesS2CPacket::abilities,
            CombatAbilitiesS2CPacket::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
