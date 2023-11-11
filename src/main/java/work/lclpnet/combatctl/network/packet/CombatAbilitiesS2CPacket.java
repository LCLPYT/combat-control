package work.lclpnet.combatctl.network.packet;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import work.lclpnet.combatctl.CombatControlMod;
import work.lclpnet.combatctl.network.CombatAbilities;

public class CombatAbilitiesS2CPacket implements FabricPacket {

    public static final PacketType<CombatAbilitiesS2CPacket> TYPE =
            PacketType.create(CombatControlMod.identifier("abilities"), CombatAbilitiesS2CPacket::new);

    private final CombatAbilities abilities;

    public CombatAbilitiesS2CPacket(CombatAbilities abilities) {
        this.abilities = abilities;
    }

    public CombatAbilitiesS2CPacket(PacketByteBuf buf) {
        this.abilities = new CombatAbilities(buf);
    }

    @Override
    public void write(PacketByteBuf buf) {
        abilities.write(buf);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public CombatAbilities getAbilities() {
        return abilities;
    }
}
