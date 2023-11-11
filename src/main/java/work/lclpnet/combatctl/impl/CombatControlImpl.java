package work.lclpnet.combatctl.impl;

import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.api.CombatDetail;
import work.lclpnet.combatctl.api.CombatStyle;
import work.lclpnet.combatctl.config.CombatControlConfig;
import work.lclpnet.combatctl.type.CombatControlPlayer;

@ApiStatus.Internal
public class CombatControlImpl implements CombatControl {

    private CombatDetailConfig defaultConfig;

    public CombatControlImpl() {
        setDefaultStyle(CombatStyle.MODERN);
    }

    @Override
    public void setDefaultStyle(CombatStyle style) {
        defaultConfig = CombatDetailConfig.getConfig(style);
    }

    @Override
    public <T extends Enum<T>> void setDefaultDetail(CombatDetail<T> detail, @NotNull T value) {
        defaultConfig = defaultConfig.with(detail, value);
    }

    @Override
    public <T extends Enum<T>> T getDefaultDetail(CombatDetail<T> detail) {
        return defaultConfig.get(detail);
    }

    @Override
    public void setStyle(ServerPlayerEntity player, @NotNull CombatStyle style) {
        CombatDetailConfig config = CombatDetailConfig.getConfig(style);

        ((CombatControlPlayer) player).combatControl$setConfig(config);
    }

    @Override
    public <T extends Enum<T>> void setDetail(ServerPlayerEntity player, CombatDetail<T> detail, @NotNull T value) {
        CombatControlPlayer ccPlayer = (CombatControlPlayer) player;

        CombatDetailConfig config = ccPlayer.combatControl$getConfig();

        if (config == null) {
            config = defaultConfig;
        }

        ccPlayer.combatControl$setConfig(config.with(detail, value));
    }

    @Override
    public <T extends Enum<T>> T getDetail(ServerPlayerEntity player, CombatDetail<T> detail) {
        CombatDetailConfig config = ((CombatControlPlayer) player).combatControl$getConfig();

        if (config == null) {
            config = defaultConfig;
        }

        return config.get(detail);
    }

    public void copyData(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer) {
        CombatDetailConfig config = ((CombatControlPlayer) oldPlayer).combatControl$getConfig();

        ((CombatControlPlayer) newPlayer).combatControl$setConfig(config);
    }

    public void update(CombatControlConfig config) {
        setDefaultStyle(config.combatStyle);
    }

    public static CombatControlImpl getInstance() {
        return Holder.instance;
    }

    private static class Holder {
        private static final CombatControlImpl instance = new CombatControlImpl();
    }
}
