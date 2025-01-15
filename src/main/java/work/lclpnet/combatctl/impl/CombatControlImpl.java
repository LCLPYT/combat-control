package work.lclpnet.combatctl.impl;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.ApiStatus;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.api.CombatStyle;
import work.lclpnet.combatctl.config.CombatControlConfig;
import work.lclpnet.combatctl.config.ConfigAccess;
import work.lclpnet.combatctl.config.GlobalConfig;
import work.lclpnet.combatctl.config.PlayerConfig;
import work.lclpnet.combatctl.network.CombatAbilities;
import work.lclpnet.combatctl.network.CombatControlNetworking;
import work.lclpnet.combatctl.network.packet.CombatAbilitiesS2CPacket;
import work.lclpnet.combatctl.type.CombatControlPlayer;

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

        for (ServerPlayerEntity player : PlayerLookup.all(server)) {
            action.accept(playerConfig(player));
        }

        update();
    }

    @Override
    public void configurePlayer(ServerPlayerEntity player, Consumer<PlayerConfig> action) {
        action.accept(playerConfig(player));
        update(player);
    }

    @Override
    public void setStyle(CombatStyle style) {
        style.configure(defaultConfig.global);
        style.configure(defaultConfig.player);

        for (ServerPlayerEntity player : PlayerLookup.all(server)) {
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
    public PlayerConfig playerConfig(ServerPlayerEntity player) {
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
        for (ServerPlayerEntity player : PlayerLookup.all(server)) {
            update(player);
        }
    }

    @Override
    public void update(ServerPlayerEntity player) {
        if (hasModInstalled(player)) {
            updateModdedPlayer(player);
        } else {
            updateVanillaPlayer(player);
        }
    }

    @Override
    public boolean hasModInstalled(ServerPlayerEntity player) {
        return networking.understands(player);
    }

    @Override
    public void copyData(ServerPlayerEntity source, ServerPlayerEntity target) {
        PlayerConfig config = ((CombatControlPlayer) source).combatControl$getConfig();

        ((CombatControlPlayer) target).combatControl$setConfig(config);

        if (source.networkHandler != target.networkHandler) {
            update(target);
        }
    }

    private CombatAbilities getAbilities(ServerPlayerEntity player) {
        var ccPlayer = (CombatControlPlayer) player;
        CombatAbilities abilities = ccPlayer.combatControl$getAbilities();

        if (abilities == null) {
            abilities = new CombatAbilities();
            ccPlayer.combatControl$setAbilities(abilities);
        }

        return abilities;
    }

    private void updateVanillaPlayer(ServerPlayerEntity player) {
        PlayerConfig config = playerConfig(player);

        // adjust the attack speed for vanilla players so that they know there is no cooldown
        EntityAttributeInstance attackSpeed = player.getAttributeInstance(EntityAttributes.ATTACK_SPEED);

        if (attackSpeed != null) {
            double value = config.isAttackCooldown() ? EntityAttributes.ATTACK_SPEED.value().getDefaultValue() : 1024;
            attackSpeed.setBaseValue(value);
        }
    }

    private void updateModdedPlayer(ServerPlayerEntity player) {
        PlayerConfig config = playerConfig(player);
        CombatAbilities abilities = getAbilities(player);

        if (abilities.syncFrom(config)) {
            var packet = new CombatAbilitiesS2CPacket(abilities);
            ServerPlayNetworking.send(player, packet);
        }
    }
}
