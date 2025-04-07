package work.lclpnet.combatctl.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import work.lclpnet.combatctl.config.PlayerConfig;

/**
 * A collection combat details that need to be known on the client side.
 * The server has control of the entries via the {@link PlayerConfig} associated with a player.
 * Data from this class is written to a packet and is sent to clients, where it is then stored and used to control client-sided features of this mod.
 */
public class CombatAbilities {

    public static final PacketCodec<PacketByteBuf, CombatAbilities> PACKET_CODEC = PacketCodec.of(CombatAbilities::write, CombatAbilities::new);

    /** Whether attack cooldown should be enabled */
    public boolean attackCooldown;
    /** If enabled, the client will register attack inputs while using items */
    public boolean attackWhileUsing;
    /** Whether the arm swing animation should render properly while using an item, should definitely be enabled when <code>attackWhileUsing=true</code>. */
    public boolean renderArmSwingWhileUsing;
    /** Skip equip animation when using items (e.g. shield) */
    public boolean noReequipWhenUsing;
    /** Whether the classic bobbing animation should be disabled */
    public boolean disableOldBobbing;

    public CombatAbilities() {
        attackCooldown = true;
        attackWhileUsing = false;
        renderArmSwingWhileUsing = false;
        noReequipWhenUsing = false;
        disableOldBobbing = false;
    }

    public CombatAbilities(PacketByteBuf buf) {
        attackCooldown = buf.readBoolean();
        attackWhileUsing = buf.readBoolean();
        renderArmSwingWhileUsing = buf.readBoolean();
        noReequipWhenUsing = buf.readBoolean();
        disableOldBobbing = buf.readBoolean();
    }

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(attackCooldown);
        buf.writeBoolean(attackWhileUsing);
        buf.writeBoolean(renderArmSwingWhileUsing);
        buf.writeBoolean(noReequipWhenUsing);
        buf.writeBoolean(disableOldBobbing);
    }

    public void copy(CombatAbilities abilities) {
        this.attackCooldown = abilities.attackCooldown;
        this.attackWhileUsing = abilities.attackWhileUsing;
        this.renderArmSwingWhileUsing = abilities.renderArmSwingWhileUsing;
        this.noReequipWhenUsing = abilities.noReequipWhenUsing;
        this.disableOldBobbing = abilities.disableOldBobbing;
    }

    /**
     * Updates the abilities depending on a given {@link PlayerConfig}.
     * The caller of this method should send an update packet to the associated client, if this method returns true.
     * @param config The player config.
     * @return True, if there were any changes to the abilities and if they should be sent to the client.
     */
    public boolean syncFrom(PlayerConfig config) {
        boolean changed = false;

        boolean b = config.isAttackCooldown();

        if (b != attackCooldown) {
            attackCooldown = b;
            changed = true;
        }

        b = config.isAttackWhileUsing();

        if (b != attackWhileUsing) {
            attackWhileUsing = b;
            changed = true;
        }

        b = config.isRenderSwingArmWhileUsing();

        if (b != renderArmSwingWhileUsing) {
            renderArmSwingWhileUsing = b;
            changed = true;
        }

        b = config.isNoReequipWhenUsing();

        if (b != noReequipWhenUsing) {
            noReequipWhenUsing = b;
            changed = true;
        }

        b = config.isDisableOldBobbing();

        if (b != disableOldBobbing) {
            disableOldBobbing = b;
            changed = true;
        }

        return changed;
    }
}
