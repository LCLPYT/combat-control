package work.lclpnet.combatctl.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

public class CombatAbilities {

    public static final PacketCodec<PacketByteBuf, CombatAbilities> PACKET_CODEC = PacketCodec.of(CombatAbilities::write, CombatAbilities::new);

    public boolean attackCooldown;
    public boolean attackWhileUsing;

    public CombatAbilities() {
        attackCooldown = true;
        attackWhileUsing = false;
    }

    public CombatAbilities(PacketByteBuf buf) {
        attackCooldown = buf.readBoolean();
        attackWhileUsing = buf.readBoolean();
    }

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(attackCooldown);
        buf.writeBoolean(attackWhileUsing);
    }

    public void copy(CombatAbilities abilities) {
        this.attackCooldown = abilities.attackCooldown;
        this.attackWhileUsing = abilities.attackWhileUsing;
    }
}
