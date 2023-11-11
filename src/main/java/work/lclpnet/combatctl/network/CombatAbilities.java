package work.lclpnet.combatctl.network;

import net.minecraft.network.PacketByteBuf;

public class CombatAbilities {

    public boolean attackCooldown;

    public CombatAbilities() {
        attackCooldown = true;
    }

    public CombatAbilities(PacketByteBuf buf) {
        attackCooldown = buf.readBoolean();
    }

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(attackCooldown);
    }

    public void copy(CombatAbilities abilities) {
        this.attackCooldown = abilities.attackCooldown;
    }
}
