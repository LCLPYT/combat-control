package work.lclpnet.combatctl.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

public class CombatAbilities {

    public static final PacketCodec<PacketByteBuf, CombatAbilities> PACKET_CODEC = PacketCodec.of(CombatAbilities::write, CombatAbilities::new);

    /** Whether attack cooldown should be enabled */
    public boolean attackCooldown;
    /** If enabled, the client will register attack inputs while using items */
    public boolean attackWhileUsing;
    /** Whether the arm swing animation should render properly while using an item, should definitely be enabled when <code>attackWhileUsing=true</code>. */
    public boolean renderArmSwingWhileUsing;

    public CombatAbilities() {
        attackCooldown = true;
        attackWhileUsing = false;
        renderArmSwingWhileUsing = false;
    }

    public CombatAbilities(PacketByteBuf buf) {
        attackCooldown = buf.readBoolean();
        attackWhileUsing = buf.readBoolean();
        renderArmSwingWhileUsing = buf.readBoolean();
    }

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(attackCooldown);
        buf.writeBoolean(attackWhileUsing);
        buf.writeBoolean(renderArmSwingWhileUsing);
    }

    public void copy(CombatAbilities abilities) {
        this.attackCooldown = abilities.attackCooldown;
        this.attackWhileUsing = abilities.attackWhileUsing;
        this.renderArmSwingWhileUsing = abilities.renderArmSwingWhileUsing;
    }
}
