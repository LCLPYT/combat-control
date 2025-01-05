package work.lclpnet.combatctl.impl;

import lombok.Getter;
import lombok.Setter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import work.lclpnet.combatctl.network.CombatAbilities;
import work.lclpnet.combatctl.network.CombatControlNetworking;
import work.lclpnet.combatctl.network.packet.CombatAbilitiesS2CPacket;

import java.util.function.Consumer;

/**
 * A configuration for combat details.
 * There is a global config that acts as a default.
 * However, the config can be adjusted for each player individually.
 * <hr>
 * A part of this class are the {@link CombatAbilities} that tell players who have the mod installed on the client,
 * whether certain client-sided features are enabled (e.g. attacking while using an item).
 * The abilities are automatically synced with the config and are send to the client if they have the mod installed.
 * <hr>
 * Edit the config preferably using {@link #edit(Consumer)}, as it batches changes to abilities and only sends one update packet.
 */
public class CombatConfig {

    /** Whether attack cooldown is enabled */
    @Getter
    private boolean attackCooldown = true;

    /** Will play modern combat hit sounds. If disabled, it just plays the classic hit sound */
    @Setter @Getter
    private boolean modernHitSounds = true;

    /** Whether modern pvp particles such as damage indicators are displayed */
    @Setter @Getter
    private boolean modernHitParticle = true;

    /** Whether sweep attacks are enabled. Even if disabled, the sweeping edge enchantment will still perform a sweep attack */
    @Setter @Getter
    private boolean sweepAttack = true;

    /** Enables fast regeneration as seen in modern Minecraft. If disabled, health regenerates every 4 seconds with at least 18 food. Full saturation also no longer regenerates health quickly. */
    @Setter @Getter
    private boolean modernRegeneration = true;

    /** Modern notch apple gives regeneration 2 and absorption 4. Disabling this gives regeneration 5 and absorption 1 instead, as it used to */
    @Setter @Getter
    private boolean modernNotchApple = true;

    /** If enabled, prevents knockback from attacks with zero damage (e.g. snowball hit) */
    @Setter @Getter
    private boolean noWeakAttackKnockBack = true;

    /** If enabled, fishing rod hits will not apply knockback */
    @Setter @Getter
    private boolean noFishingRodKnockBack = true;

    /** If disabled, entities attacked in the air will take more knockback */
    @Setter @Getter
    private boolean strongKnockBackInAir = false;

    /** If enabled, critical hits will not be possible while sprinting */
    @Setter @Getter
    private boolean noSprintCriticalHits = true;

    /** If enabled, attacking will stop sprinting */
    @Setter @Getter
    private boolean noAttackSprinting = true;

    /** If enabled, fishing rod pulls will apply a slight upwards boost */
    @Setter @Getter
    private boolean fishingRodLaunch = false;

    /** If enabled, hooking an entity will cause 5 damage to a fishing rod. Otherwise, only 3 damage are applied to the rod. */
    @Setter @Getter
    private boolean modernFishingRodDurability = true;

    /** Whether attack is allowed while using an item (e.g. aiming a bow or eating food) used to be possible in 1.7.10 and before */
    @Getter
    private boolean attackWhileUsing = false;

    /** Whether the arm swing animation should render properly while using an item, should definitely be enabled when <code>attackWhileUsing=true</code> */
    @Getter
    private boolean renderSwingArmWhileUsing = false;

    /** If enabled, attacking will damage the held item by 2, otherwise only by 1 */
    @Setter @Getter
    private boolean modernItemDurability = true;

    /** If enabled, the sharpness enchantment adds 0.5 damage per level. If disabled, it is 1.25 damage per level */
    @Setter @Getter
    private boolean modernSharpness = true;

    /** Skip equip animation when using items (e.g. shield) */
    @Getter
    private boolean noReequipWhenUsing = false;

    /** If enabled, the fishing rod will move slower, as seen in 1.9+ versions */
    @Setter @Getter
    private boolean slowFishingRodMotion = true;

    /** If enabled, plays the modern fishing rod reeling sounds */
    @Setter @Getter
    private boolean modernFishingRodSounds = true;

    /** Whether reeling in an entity pulls it towards the player. Many PVP servers disabled this, however it was always enabled in vanilla */
    @Setter @Getter
    private boolean fishingRodPull = true;

    /* ----- */

    private final ServerPlayNetworkHandler networkHandler;
    private final boolean listening;
    private final CombatAbilities abilities = new CombatAbilities();
    private boolean autoUpdate = true;
    private boolean dirty = false;

    public CombatConfig(ServerPlayerEntity player) {
        this.networkHandler = player.networkHandler;
        this.listening = CombatControlNetworking.isListening(player);
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
        EntityAttributeInstance attackSpeed = networkHandler.player.getAttributeInstance(EntityAttributes.ATTACK_SPEED);

        if (attackSpeed == null) return;

        double value = attackCooldown ? EntityAttributes.ATTACK_SPEED.value().getDefaultValue() : 1024;

        attackSpeed.setBaseValue(value);
    }

    public void setAttackWhileUsing(boolean attackWhileUsing) {
        if (this.attackWhileUsing == attackWhileUsing) return;

        this.attackWhileUsing = attackWhileUsing;
        abilities.attackWhileUsing = attackWhileUsing;

        onSync();
    }

    public void setRenderSwingArmWhileUsing(boolean renderSwingArmWhileUsing) {
        if (this.renderSwingArmWhileUsing == renderSwingArmWhileUsing) return;

        this.renderSwingArmWhileUsing = renderSwingArmWhileUsing;
        abilities.renderArmSwingWhileUsing = renderSwingArmWhileUsing;

        onSync();
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
        ServerPlayNetworking.send(networkHandler.player, packet);
    }
}
