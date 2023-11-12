package work.lclpnet.combatctl.impl;

import org.jetbrains.annotations.ApiStatus;
import work.lclpnet.combatctl.api.CombatStyle;
import work.lclpnet.combatctl.api.GlobalCombatControl;
import work.lclpnet.combatctl.config.CombatControlConfig;

@ApiStatus.Internal
public class GlobalCombatControlImpl implements GlobalCombatControl {

    private final CombatGlobalConfig globalConfig;

    public GlobalCombatControlImpl() {
        this.globalConfig = new CombatGlobalConfig();
    }

    @Override
    public CombatGlobalConfig getGlobalConfig() {
        return globalConfig;
    }

    public void setStyle(CombatStyle style) {
        applyStyle(style, globalConfig);
    }

    private void applyStyle(CombatStyle style, CombatGlobalConfig config) {
        boolean modern = style == CombatStyle.MODERN;

        config.setModernDamageValues(modern);
        config.setModernSharpness(modern);
    }

    public void update(CombatControlConfig config) {
        setStyle(config.combatStyle);
    }

    public static GlobalCombatControlImpl get() {
        return Holder.instance;
    }

    private static class Holder {
        private static final GlobalCombatControlImpl instance = new GlobalCombatControlImpl();
    }
}
