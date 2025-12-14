package work.lclpnet.combatctl.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import work.lclpnet.combatctl.CCModInit;
import work.lclpnet.combatctl.network.CombatAbilities;

public record CombatAbilitiesS2CPacket(CombatAbilities abilities) implements CustomPacketPayload {

    public static final Type<CombatAbilitiesS2CPacket> ID = new Type<>(CCModInit.identifier("abilities"));

    public static final StreamCodec<FriendlyByteBuf, CombatAbilitiesS2CPacket> CODEC = StreamCodec.composite(
            CombatAbilities.PACKET_CODEC, CombatAbilitiesS2CPacket::abilities,
            CombatAbilitiesS2CPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
