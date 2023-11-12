package work.lclpnet.combatctl.impl;

public class CombatGlobalConfig {

    private boolean modernDamageValues = true;
    private boolean modernSharpness = true;
    private boolean largerHitboxes = false;

    public boolean isModernDamageValues() {
        return modernDamageValues;
    }

    public void setModernDamageValues(boolean modernDamageValues) {
        this.modernDamageValues = modernDamageValues;
    }

    public boolean isModernSharpness() {
        return modernSharpness;
    }

    public void setModernSharpness(boolean modernSharpness) {
        this.modernSharpness = modernSharpness;
    }

    public boolean isLargerHitboxes() {
        return largerHitboxes;
    }

    public void setLargerHitboxes(boolean largerHitboxes) {
        this.largerHitboxes = largerHitboxes;
    }
}
