package work.lclpnet.combatctl.impl;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.ApiStatus;
import work.lclpnet.combatctl.network.CombatAbilities;
import work.lclpnet.combatctl.network.CombatControlNetworking;
import work.lclpnet.combatctl.network.packet.CombatAbilitiesS2CPacket;

import java.util.function.Consumer;

/**
 * An immutable configuration for combat details.
 */
@ApiStatus.Internal
public class CombatConfig {

    private final ServerPlayerEntity player;
    private final boolean listening;
    private final CombatAbilities abilities = new CombatAbilities();
    private boolean autoUpdate = true;
    private boolean dirty = false;
    private boolean attackCooldown = true;

    public CombatConfig(ServerPlayerEntity player) {
        this.player = player;
        this.listening = CombatControlNetworking.isListening(player);
    }

    public boolean isAttackCooldown() {
        return attackCooldown;
    }

    public void setAttackCooldown(boolean attackCooldown) {
        if (this.attackCooldown == attackCooldown) return;

        this.attackCooldown = attackCooldown;

        if (listening) {
            onSync();
            return;
        }

        // for player who don't have the mod, adjust the attack speed value so that they know there is no cooldown
        EntityAttributeInstance attackSpeed = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        if (attackSpeed == null) return;

        double value = attackCooldown ? EntityAttributes.GENERIC_ATTACK_SPEED.getDefaultValue() : 1024;

        attackSpeed.setBaseValue(value);
    }

    public void edit(Consumer<CombatConfig> action) {
        autoUpdate = false;

        action.accept(this);

        if (dirty) {
            dirty = false;
            syncAbilities();
        }

        autoUpdate = true;
    }

    private void onSync() {
        if (!autoUpdate) {
            dirty = true;
            return;
        }

        syncAbilities();
    }

    public void syncAbilities() {
        if (!listening) return;

        var packet = new CombatAbilitiesS2CPacket(abilities);
        CombatControlNetworking.send(player, packet);
    }
}