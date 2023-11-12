package work.lclpnet.combatctl.network;

import net.minecraft.network.PacketByteBuf;

public class CombatAbilities {

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
