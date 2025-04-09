package work.lclpnet.combatctl.impl;

import net.minecraft.util.Identifier;
import work.lclpnet.combatctl.api.CombatStyle;
import work.lclpnet.combatctl.api.KnockbackVariant;
import work.lclpnet.combatctl.config.GlobalConfig;
import work.lclpnet.combatctl.config.PlayerConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CombatStyles {

    public static final CombatStyle CLASSIC = new ModernStyle(false), MODERN = new ModernStyle(true);
    private static final Map<Identifier, CombatStyle> registry = new HashMap<>();

    static {
        register(Identifier.ofVanilla("classic"), CLASSIC);
        register(Identifier.ofVanilla("modern"), MODERN);
    }

    public static void register(Identifier id, CombatStyle style) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(style);

        registry.put(id, style);
    }

    public static Map<Identifier, CombatStyle> registry() {
        return Collections.unmodifiableMap(registry);
    }

    private record ModernStyle(boolean modern) implements CombatStyle {

        @Override
        public void configure(PlayerConfig player) {
            player.setAttackCooldown(modern);
            player.setModernHitSounds(modern);
            player.setModernHitParticle(modern);
            player.setSweepAttack(modern);
            player.setModernRegeneration(modern);
            player.setModernNotchApple(modern);
            player.setNoWeakAttackKnockBack(modern);
            player.setNoFishingRodKnockBack(modern);
            player.setNoSprintCriticalHits(modern);
            player.setNoAttackSprinting(modern);
            player.setFishingRodLaunch(!modern);
            player.setModernFishingRodDurability(modern);
            player.setAttackWhileUsing(!modern);
            player.setRenderSwingArmWhileUsing(!modern);
            player.setModernItemDurability(modern);
            player.setSlowFishingRodMotion(modern);
            player.setModernFishingRodSounds(modern);
            player.setModernSharpness(modern);
            player.setNoReequipWhenUsing(!modern);
            player.setFishingRodPull(modern);
            player.setSwordBlocking(!modern);
            player.setKnockbackVariant(modern ? KnockbackVariant.DEFAULT : KnockbackVariant.NO_SCALING);
            player.setModernDamageValues(modern);
        }

        @Override
        public void configure(GlobalConfig global) {
            global.setLargerHitboxes(!modern);
        }
    }
}
