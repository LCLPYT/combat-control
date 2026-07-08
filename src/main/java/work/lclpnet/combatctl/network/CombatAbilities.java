package work.lclpnet.combatctl.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import work.lclpnet.combatctl.config.GlobalConfig;
import work.lclpnet.combatctl.config.PlayerConfig;

/**
 * A collection combat details that need to be known on the client side.
 * The server has control of the entries via the {@link PlayerConfig} associated with a player.
 * Data from this class is written to a packet and is sent to clients, where it is then stored and used to control client-sided features of this mod.
 */
public class CombatAbilities {

    public static final StreamCodec<FriendlyByteBuf, CombatAbilities> PACKET_CODEC = StreamCodec.ofMember(CombatAbilities::write, CombatAbilities::new);

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
    /** Whether to use the new sharpness bonus damage formula or the classic one. */
    public boolean modernSharpness;
    /** Whether the server enables larger hitboxes or not. */
    public boolean largerHitboxes;

    public CombatAbilities() {
        attackCooldown = true;
        attackWhileUsing = false;
        renderArmSwingWhileUsing = false;
        noReequipWhenUsing = false;
        disableOldBobbing = false;
        modernSharpness = true;
        largerHitboxes = false;
    }

    public CombatAbilities(FriendlyByteBuf buf) {
        attackCooldown = buf.readBoolean();
        attackWhileUsing = buf.readBoolean();
        renderArmSwingWhileUsing = buf.readBoolean();
        noReequipWhenUsing = buf.readBoolean();
        disableOldBobbing = buf.readBoolean();
        modernSharpness = buf.readBoolean();
        largerHitboxes = buf.readBoolean();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(attackCooldown);
        buf.writeBoolean(attackWhileUsing);
        buf.writeBoolean(renderArmSwingWhileUsing);
        buf.writeBoolean(noReequipWhenUsing);
        buf.writeBoolean(disableOldBobbing);
        buf.writeBoolean(modernSharpness);
        buf.writeBoolean(largerHitboxes);
    }

    public void copy(CombatAbilities abilities) {
        this.attackCooldown = abilities.attackCooldown;
        this.attackWhileUsing = abilities.attackWhileUsing;
        this.renderArmSwingWhileUsing = abilities.renderArmSwingWhileUsing;
        this.noReequipWhenUsing = abilities.noReequipWhenUsing;
        this.disableOldBobbing = abilities.disableOldBobbing;
        this.modernSharpness = abilities.modernSharpness;
        this.largerHitboxes = abilities.largerHitboxes;
    }

    /**
     * Updates the abilities depending on a given {@link PlayerConfig} and {@link GlobalConfig}.
     * The caller of this method should send an update packet to the associated client, if this method returns true.
     *
     * @param config The player config.
     * @param globalConfig The global config.
     * @return True, if there were any changes to the abilities and if they should be sent to the client.
     */
    public boolean syncFrom(PlayerConfig config, GlobalConfig globalConfig) {
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

        b = config.isModernSharpness();

        if (b != modernSharpness) {
            modernSharpness = b;
            changed = true;
        }

        b = globalConfig.isLargerHitboxes();

        if (b != largerHitboxes) {
            largerHitboxes = b;
            changed = true;
        }

        return changed;
    }
}
