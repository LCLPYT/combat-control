package work.lclpnet.combatctl.impl;

public class CombatGlobalConfig {

    private boolean modernDamageValues = true;
    private boolean modernSharpness = true;

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
}
