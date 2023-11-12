package work.lclpnet.combatctl.impl;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.api.CombatStyle;
import work.lclpnet.combatctl.config.ConfigAccess;
import work.lclpnet.combatctl.type.CombatControlPlayer;

@ApiStatus.Internal
public class CombatControlImpl implements CombatControl {

    private final MinecraftServer server;
    private CombatStyle defaultStyle;

    public CombatControlImpl(MinecraftServer server, ConfigAccess configAccess) {
        this.server = server;
        this.defaultStyle = configAccess.getConfig().combatStyle;
    }

    @Override
    public void setStyle(CombatStyle style) {
        defaultStyle = style;

        for (ServerPlayerEntity player : PlayerLookup.all(server)) {
            setStyle(player, style);
        }
    }

    @Override
    public CombatStyle getStyle() {
        return defaultStyle;
    }

    @Override
    public void setStyle(ServerPlayerEntity player, @NotNull CombatStyle style) {
        applyStyle(style, getConfig(player));
    }

    @Override
    public CombatConfig getConfig(ServerPlayerEntity player) {
        CombatControlPlayer ccPlayer = (CombatControlPlayer) player;
        CombatConfig config = ccPlayer.combatControl$getConfig();

        if (config == null) {
            config = new CombatConfig(player);
            ccPlayer.combatControl$setConfig(config);
            applyStyle(defaultStyle, config);
        }

        return config;
    }

    private void applyStyle(CombatStyle style, CombatConfig config) {
        boolean modern = style == CombatStyle.MODERN;

        config.edit(cfg -> {
            config.setAttackCooldown(modern);
            config.setModernHitSounds(modern);
            config.setModernHitParticle(modern);
            config.setSweepAttack(modern);
            config.setModernRegeneration(modern);
            config.setModernNotchApple(modern);
            config.setPreventWeakAttackKnockBack(modern);
            config.setPreventFishingRodKnockBack(modern);
        });
    }

    @Override
    public void copyData(ServerPlayerEntity source, ServerPlayerEntity target) {
        CombatConfig config = ((CombatControlPlayer) source).combatControl$getConfig();

        ((CombatControlPlayer) target).combatControl$setConfig(config);
    }
}
