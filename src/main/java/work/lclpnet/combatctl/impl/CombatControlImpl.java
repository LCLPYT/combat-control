package work.lclpnet.combatctl.impl;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.ApiStatus;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.api.CombatStyle;
import work.lclpnet.combatctl.config.CombatControlConfig;
import work.lclpnet.combatctl.config.GlobalConfig;
import work.lclpnet.combatctl.config.PlayerConfig;
import work.lclpnet.combatctl.network.CombatAbilities;
import work.lclpnet.combatctl.network.CombatControlNetworking;
import work.lclpnet.combatctl.network.packet.CombatAbilitiesS2CPacket;
import work.lclpnet.combatctl.type.CombatControlPlayer;
import work.lclpnet.kibu.config.ConfigAccess;

import java.util.function.Consumer;

@ApiStatus.Internal
public class CombatControlImpl implements CombatControl {

    private final MinecraftServer server;
    private final ConfigAccess<CombatControlConfig> configAccess;
    private final CombatControlNetworking networking;
    private final CombatControlConfig defaultConfig;

    public CombatControlImpl(MinecraftServer server, ConfigAccess<CombatControlConfig> configAccess, CombatControlNetworking networking) {
        this.server = server;
        this.configAccess = configAccess;
        this.networking = networking;
        this.defaultConfig = configAccess.config();
    }

    @Override
    public void configurePlayers(Consumer<PlayerConfig> action) {
        action.accept(defaultConfig.player);

        for (ServerPlayer player : PlayerLookup.all(server)) {
            action.accept(playerConfig(player));
        }

        update();
    }

    @Override
    public void configurePlayer(ServerPlayer player, Consumer<PlayerConfig> action) {
        action.accept(playerConfig(player));
        update(player);
    }

    @Override
    public void setStyle(CombatStyle style) {
        style.configure(defaultConfig.global);
        style.configure(defaultConfig.player);

        for (ServerPlayer player : PlayerLookup.all(server)) {
            style.configure(playerConfig(player));
        }

        update();
    }

    @Override
    public GlobalConfig globalConfig() {
        return defaultConfig.global;
    }

    @Override
    public PlayerConfig playerConfig() {
        return defaultConfig.player;
    }

    @Override
    public PlayerConfig playerConfig(ServerPlayer player) {
        var ccPlayer = (CombatControlPlayer) player;
        PlayerConfig config = ccPlayer.combatControl$getConfig();

        if (config != null) {
            return config;
        }

        synchronized (this) {
            config = ccPlayer.combatControl$getConfig();

            if (config == null) {
                config = defaultConfig.player.clone();
                ccPlayer.combatControl$setConfig(config);
            }
        }

        return config;
    }

    @Override
    public void update() {
        updatePlayers();
        configAccess.save();
    }

    public void updatePlayers() {
        for (ServerPlayer player : PlayerLookup.all(server)) {
            update(player);
        }
    }

    @Override
    public void update(ServerPlayer player) {
        if (hasModInstalled(player)) {
            updateModdedPlayer(player);
        } else {
            updateVanillaPlayer(player);
        }

        DynamicItemHandler.getInstance().update(player);
    }

    @Override
    public synchronized void resetPlayerConfig(ServerPlayer player) {
        ((CombatControlPlayer) player).combatControl$setConfig(defaultConfig.player.clone());
    }

    @Override
    public boolean hasModInstalled(ServerPlayer player) {
        return networking.understands(player);
    }

    @Override
    public void copyData(ServerPlayer source, ServerPlayer target) {
        PlayerConfig config = ((CombatControlPlayer) source).combatControl$getConfig();

        synchronized (this) {
            ((CombatControlPlayer) target).combatControl$setConfig(config);
        }

        if (source.connection != target.connection) {
            update(target);
        }
    }

    private CombatAbilities getAbilities(ServerPlayer player) {
        var ccPlayer = (CombatControlPlayer) player;
        CombatAbilities abilities = ccPlayer.combatControl$getAbilities();

        if (abilities == null) {
            abilities = new CombatAbilities();
            ccPlayer.combatControl$setAbilities(abilities);
        }

        return abilities;
    }

    private void updateVanillaPlayer(ServerPlayer player) {
        PlayerConfig config = playerConfig(player);

        // adjust the attack speed for vanilla players so that they know there is no cooldown
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);

        if (attackSpeed != null) {
            double value = config.isAttackCooldown() ? Attributes.ATTACK_SPEED.value().getDefaultValue() : 1024;
            attackSpeed.setBaseValue(value);
        }
    }

    private void updateModdedPlayer(ServerPlayer player) {
        PlayerConfig config = playerConfig(player);
        CombatAbilities abilities = getAbilities(player);

        if (abilities.syncFrom(config)) {
            var packet = new CombatAbilitiesS2CPacket(abilities);
            ServerPlayNetworking.send(player, packet);
        }

        // reset attack speed, if player was not modded anytime before
        if (config.isAttackCooldown()) {
            AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);

            if (attr != null) {
                attr.setBaseValue(Attributes.ATTACK_SPEED.value().getDefaultValue());
            }
        }
    }
}
