package work.lclpnet.combatctl.impl;

public class CombatGlobalConfig {

    /** If enabled, tools like axes will deal the modern amount of damage that takes cooldown into account. If disabled, damage values will be reverted / adapted to 1.8 and previous versions */
    private boolean modernDamageValues = true;
    /** Expand hitboxes by 10% to make hits more accurate */
    private boolean largerHitboxes = false;

    public boolean isModernDamageValues() {
        return modernDamageValues;
    }

    public void setModernDamageValues(boolean modernDamageValues) {
        this.modernDamageValues = modernDamageValues;
    }

    public boolean isLargerHitboxes() {
        return largerHitboxes;
    }

    public void setLargerHitboxes(boolean largerHitboxes) {
        this.largerHitboxes = largerHitboxes;
    }
}
