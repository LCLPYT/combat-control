package work.lclpnet.combatctl.impl;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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

    /** Whether attack cooldown is enabled */
    private boolean attackCooldown = true;
    /** Will play modern combat hit sounds. If disabled, it just plays the classic hit sound */
    private boolean modernHitSounds = true;
    /** Whether modern pvp particles such as damage indicators are displayed */
    private boolean modernHitParticle = true;
    /** Whether sweep attacks are enabled. Even if disabled, the sweeping edge enchantment will still perform a sweep attack */
    private boolean sweepAttack = true;
    /** Enables fast regeneration as seen in modern Minecraft. If disabled, health regenerates every 4 seconds with at least 18 food. Full saturation also no longer regenerates health quickly. */
    private boolean modernRegeneration = true;
    /** Modern notch apple gives regeneration 2 and absorption 4. Disabling this gives regeneration 5 and absorption 1 instead, as it used to */
    private boolean modernNotchApple = true;
    /** If enabled, prevents knockback from attacks with zero damage (e.g. snowball hit) */
    private boolean noWeakAttackKnockBack = true;
    /** If enabled, fishing rod hits will not apply knockback */
    private boolean noFishingRodKnockBack = true;
    /** If disabled, entities attacked in the air will take more knockback */
    private boolean strongKnockBackInAir = false;
    /** If enabled, critical hits will not be possible while sprinting */
    private boolean noSprintCriticalHits = true;
    /** If enabled, attacking will stop sprinting */
    private boolean noAttackSprinting = true;
    /** If enabled, fishing rod pulls will apply a slight upwards boost */
    private boolean fishingRodLaunch = false;
    /** If enabled, hooking an entity will cause 5 damage to a fishing rod. Otherwise, only 3 damage are applied to the rod. */
    private boolean modernFishingRodDurability = true;
    /** Whether attack is allowed while using an item (e.g. aiming a bow or eating food) */
    private boolean attackWhileUsing = false;
    /** Whether the arm swing animation should render properly while using an item, should definitely be enabled when <code>attackWhileUsing=true</code> */
    private boolean renderSwingArmWhileUsing = false;
    /** If enabled, attacking will damage the held item by 2, otherwise only by 1 */
    private boolean modernItemDurability = true;
    /** If enabled, the fishing rod will move slower, as seen in 1.9+ versions */
    private boolean slowFishingRodMotion = true;
    /** If enabled, plays the modern fishing rod reeling sounds */
    private boolean modernFishingRodSounds = true;
    /** If enabled, the sharpness enchantment adds 0.5 damage per level. If disabled, it is 1.25 damage per level */
    private boolean modernSharpness = true;
    /** Skip equip animation when using items (e.g. shield) */
    private boolean noReequipWhenUsing = false;

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
        EntityAttributeInstance attackSpeed = player.getAttributeInstance(EntityAttributes.ATTACK_SPEED);
        if (attackSpeed == null) return;

        double value = attackCooldown ? EntityAttributes.ATTACK_SPEED.value().getDefaultValue() : 1024;

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

    public boolean isRenderSwingArmWhileUsing() {
        return renderSwingArmWhileUsing;
    }

    public void setRenderSwingArmWhileUsing(boolean renderSwingArmWhileUsing) {
        if (this.renderSwingArmWhileUsing == renderSwingArmWhileUsing) return;

        this.renderSwingArmWhileUsing = renderSwingArmWhileUsing;
        abilities.renderArmSwingWhileUsing = renderSwingArmWhileUsing;

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

    public boolean isModernSharpness() {
        return modernSharpness;
    }

    public void setModernSharpness(boolean modernSharpness) {
        this.modernSharpness = modernSharpness;
    }

    public boolean isNoReequipWhenUsing() {
        return noReequipWhenUsing;
    }

    public void setNoReequipWhenUsing(boolean noReequipWhenUsing) {
        if (this.noReequipWhenUsing == noReequipWhenUsing) return;

        this.noReequipWhenUsing = noReequipWhenUsing;
        abilities.noReequipWhenUsing = noReequipWhenUsing;

        onSync();
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
        ServerPlayNetworking.send(player, packet);
    }
}
