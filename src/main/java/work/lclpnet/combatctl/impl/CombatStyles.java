package work.lclpnet.combatctl.impl;

import work.lclpnet.combatctl.api.CombatStyle;
import work.lclpnet.combatctl.config.GlobalConfig;
import work.lclpnet.combatctl.config.PlayerConfig;

public class CombatStyles {

    public static final CombatStyle CLASSIC = new ModernStyle(false), MODERN = new ModernStyle(true);

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
            player.setStrongKnockBackInAir(!modern);
            player.setNoSprintCriticalHits(modern);
            player.setNoAttackSprinting(modern);
            player.setFishingRodLaunch(!modern);
            player.setModernFishingRodDurability(modern);
            player.setAttackWhileUsing(false);  // was available in 1.7.10, but not in 1.8.9, therefore opt-in
            player.setRenderSwingArmWhileUsing(!modern);
            player.setModernItemDurability(modern);
            player.setSlowFishingRodMotion(modern);
            player.setModernFishingRodSounds(modern);
            player.setModernSharpness(modern);
            player.setNoReequipWhenUsing(!modern);
            player.setFishingRodPull(false);  // wasn't enabled in vanilla, but usually enabled on pvp-servers, opt-in
        }

        @Override
        public void configure(GlobalConfig global) {
            global.setModernDamageValues(modern);
            global.setLargerHitboxes(!modern);
        }
    }
}
