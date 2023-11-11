package work.lclpnet.combatctl.api;

import org.jetbrains.annotations.Nullable;

public enum CombatStyle {

    OLD("old"),
    MODERN("modern");

    private final String value;

    CombatStyle(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Nullable
    public static CombatStyle tryFrom(String value) {
        for (CombatStyle style : values()) {
            if (style.value.equals(value)) {
                return style;
            }
        }

        return null;
    }
}
