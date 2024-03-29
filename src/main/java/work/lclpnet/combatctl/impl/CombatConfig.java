package work.lclpnet.combatctl.impl;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import work.lclpnet.combatctl.network.CombatAbilities;
import work.lclpnet.combatctl.network.CombatControlNetworking;
import work.lclpnet.combatctl.network.packet.CombatAbilitiesS2CPacket;

import java.util.function.Consumer;

/**
 * An immutable configuration for combat details.
 */
public class CombatConfig {

    private final ServerPlayerEntity player;
    private final boolean listening;
    private final CombatAbilities abilities = new CombatAbilities();
    private boolean autoUpdate = true;
    private boolean dirty = false;
    private boolean attackCooldown = true;
    private boolean modernHitSounds = true;
    private boolean modernHitParticle = true;
    private boolean sweepAttack = true;
    private boolean modernRegeneration = true;
    private boolean modernNotchApple = true;
    private boolean noWeakAttackKnockBack = true;
    private boolean noFishingRodKnockBack = true;
    private boolean strongKnockBackInAir = false;
    private boolean noSprintCriticalHits = true;
    private boolean noAttackSprinting = true;
    private boolean fishingRodLaunch = false;
    private boolean modernFishingRodDurability = true;
    private boolean attackWhileUsing = false;
    private boolean modernItemDurability = true;
    private boolean slowFishingRodMotion = true;
    private boolean modernFishingRodSounds = true;

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
        abilities.attackCooldown = attackCooldown;

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

    public boolean isModernHitSounds() {
        return modernHitSounds;
    }

    public void setModernHitSounds(boolean modernHitSounds) {
        this.modernHitSounds = modernHitSounds;
    }

    public boolean isModernHitParticle() {
        return modernHitParticle;
    }

    public void setModernHitParticle(boolean modernHitParticle) {
        this.modernHitParticle = modernHitParticle;
    }

    public boolean isSweepAttack() {
        return sweepAttack;
    }

    public void setSweepAttack(boolean sweepAttack) {
        this.sweepAttack = sweepAttack;
    }

    public boolean isModernRegeneration() {
        return modernRegeneration;
    }

    public void setModernRegeneration(boolean modernRegeneration) {
        this.modernRegeneration = modernRegeneration;
    }

    public boolean isModernNotchApple() {
        return modernNotchApple;
    }

    public void setModernNotchApple(boolean modernNotchApple) {
        this.modernNotchApple = modernNotchApple;
    }

    public boolean isNoWeakAttackKnockBack() {
        return noWeakAttackKnockBack;
    }

    public void setNoWeakAttackKnockBack(boolean noWeakAttackKnockBack) {
        this.noWeakAttackKnockBack = noWeakAttackKnockBack;
    }

    public boolean isNoFishingRodKnockBack() {
        return noFishingRodKnockBack;
    }

    public void setNoFishingRodKnockBack(boolean noFishingRodKnockBack) {
        this.noFishingRodKnockBack = noFishingRodKnockBack;
    }

    public boolean isStrongKnockBackInAir() {
        return strongKnockBackInAir;
    }

    public void setStrongKnockBackInAir(boolean strongKnockBackInAir) {
        this.strongKnockBackInAir = strongKnockBackInAir;
    }

    public boolean isNoSprintCriticalHits() {
        return noSprintCriticalHits;
    }

    public void setNoSprintCriticalHits(boolean noSprintCriticalHits) {
        this.noSprintCriticalHits = noSprintCriticalHits;
    }

    public boolean isNoAttackSprinting() {
        return noAttackSprinting;
    }

    public void setNoAttackSprinting(boolean noAttackSprinting) {
        this.noAttackSprinting = noAttackSprinting;
    }

    public boolean isFishingRodLaunch() {
        return fishingRodLaunch;
    }

    public void setFishingRodLaunch(boolean fishingRodLaunch) {
        this.fishingRodLaunch = fishingRodLaunch;
    }

    public boolean isModernFishingRodDurability() {
        return modernFishingRodDurability;
    }

    public void setModernFishingRodDurability(boolean modernFishingRodDurability) {
        this.modernFishingRodDurability = modernFishingRodDurability;
    }

    public boolean isAttackWhileUsing() {
        return attackWhileUsing;
    }

    public void setAttackWhileUsing(boolean attackWhileUsing) {
        if (this.attackWhileUsing == attackWhileUsing) return;

        this.attackWhileUsing = attackWhileUsing;
        abilities.attackWhileUsing = attackWhileUsing;

        onSync();
    }

    public boolean isModernItemDurability() {
        return modernItemDurability;
    }

    public void setModernItemDurability(boolean modernItemDurability) {
        this.modernItemDurability = modernItemDurability;
    }

    public boolean isSlowFishingRodMotion() {
        return slowFishingRodMotion;
    }

    public void setSlowFishingRodMotion(boolean slowFishingRodMotion) {
        this.slowFishingRodMotion = slowFishingRodMotion;
    }

    public boolean isModernFishingRodSounds() {
        return modernFishingRodSounds;
    }

    public void setModernFishingRodSounds(boolean modernFishingRodSounds) {
        this.modernFishingRodSounds = modernFishingRodSounds;
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
