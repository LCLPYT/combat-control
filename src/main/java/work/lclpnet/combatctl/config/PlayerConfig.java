package work.lclpnet.combatctl.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeComment;
import lombok.Getter;
import lombok.Setter;
import work.lclpnet.combatctl.api.KnockbackVariant;

/**
 * A configuration of player specific combat details that is available within the server context.
 * Combat control features a global player config that is persisted in the mod configuration.
 * Each player is assigned an independent copy of the global config upon joining.
 */
@Getter @Setter
public class PlayerConfig implements Cloneable {

    @SerdeComment("Whether attack cooldown is enabled")
    private boolean attackCooldown = true;

    @SerdeComment("Will play modern combat hit sounds. If disabled, it just plays the classic hit sound")
    private boolean modernHitSounds = true;

    @SerdeComment("Whether modern pvp particles such as damage indicators are displayed")
    private boolean modernHitParticle = true;

    @SerdeComment("Whether sweep attacks are enabled. Even if disabled, the sweeping edge enchantment will still perform a sweep attack")
    private boolean sweepAttack = true;

    @SerdeComment("Enables fast regeneration as seen in modern Minecraft. If disabled, health regenerates every 4 seconds with at least 18 food. Full saturation also no longer regenerates health quickly.")
    private boolean modernRegeneration = true;

    @SerdeComment("Modern notch apple gives regeneration 2 and absorption 4. Disabling this gives regeneration 5 and absorption 1 instead, as it used to")
    private boolean modernNotchApple = true;

    @SerdeComment("If enabled, prevents knockback from attacks with zero damage (e.g. snowball hit)")
    private boolean noWeakAttackKnockBack = true;

    @SerdeComment("If enabled, fishing rod hits will not apply knockback")
    private boolean noFishingRodKnockBack = true;

    @SerdeComment("If enabled, critical hits will not be possible while sprinting")
    private boolean noSprintCriticalHits = true;

    @SerdeComment("If enabled, attacking will stop sprinting")
    private boolean noAttackSprinting = true;

    @SerdeComment("If enabled, fishing rod pulls will apply a slight upwards boost")
    private boolean fishingRodLaunch = false;

    @SerdeComment("If enabled, hooking an entity will cause 5 damage to a fishing rod. Otherwise, only 3 damage are applied to the rod.")
    private boolean modernFishingRodDurability = true;

    @SerdeComment("Whether attack is allowed while using an item (e.g. aiming a bow or eating food) used to be possible in 1.7.10 and before")
    private boolean attackWhileUsing = false;

    @SerdeComment("Whether the arm swing animation should render properly while using an item, should definitely be enabled when attackWhileUsing=true")
    private boolean renderSwingArmWhileUsing = false;

    @SerdeComment("If enabled, attacking will damage the held item by 2, otherwise only by 1")
    private boolean modernItemDurability = true;

    @SerdeComment("If enabled, the sharpness enchantment adds 0.5 damage per level. If disabled, it is 1.25 damage per level")
    private boolean modernSharpness = true;

    @SerdeComment("Skip equip animation when using items (e.g. shield)")
    private boolean noReequipWhenUsing = false;

    @SerdeComment("If enabled, the fishing rod will move slower, as seen in 1.9+ versions")
    private boolean slowFishingRodMotion = true;

    @SerdeComment("If enabled, plays the modern fishing rod reeling sounds")
    private boolean modernFishingRodSounds = true;

    @SerdeComment("Whether reeling in an entity pulls it towards the player. Many PVP servers disabled this, however it was always enabled in vanilla")
    private boolean fishingRodPull = true;

    @SerdeComment("Allows the player to block using a sword. Unlike when blocking with a shield, the damage is only reduced partially. Players without the mod can also use this feature, but will receive a temporary shield instead.")
    private boolean swordBlocking = false;

    @SerdeComment("Determines how the player receives knockback.")
    private KnockbackVariant knockbackVariant = KnockbackVariant.DEFAULT;

    /* ----- */

    @Override
    public PlayerConfig clone() {
        try {
            return (PlayerConfig) super.clone();  // shallow-copy
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone failed", e);
        }
    }
}
