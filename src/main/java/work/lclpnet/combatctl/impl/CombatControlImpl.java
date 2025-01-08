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
import work.lclpnet.combatctl.config.CombatConfig;
import work.lclpnet.combatctl.config.CombatControlConfig;
import work.lclpnet.combatctl.config.ConfigAccess;
import work.lclpnet.combatctl.network.CombatAbilities;
import work.lclpnet.combatctl.network.CombatControlNetworking;
import work.lclpnet.combatctl.network.packet.CombatAbilitiesS2CPacket;
import work.lclpnet.combatctl.type.CombatControlPlayer;

import java.util.function.Consumer;

@ApiStatus.Internal
public class CombatControlImpl implements CombatControl {

    private final MinecraftServer server;
    private final CombatControlConfig defaultConfig;

    public CombatControlImpl(MinecraftServer server, ConfigAccess configAccess) {
        this.server = server;
        this.defaultConfig = configAccess.config();
    }

    @Override
    public void setStyle(CombatStyle style) {
        style.configure(defaultConfig);

        for (ServerPlayerEntity player : PlayerLookup.all(server)) {
            setStyle(player, style);
        }
    }

    @Override
    public CombatConfig getConfig(ServerPlayerEntity player) {
        var ccPlayer = (CombatControlPlayer) player;
        CombatConfig config = ccPlayer.combatControl$getConfig();

        if (config == null) {
            config = this.defaultConfig.player.clone();
            ccPlayer.combatControl$setConfig(config);
        }

        return config;
    }

    @Override
    public void configure(ServerPlayerEntity player, Consumer<CombatConfig> action) {
        action.accept(getConfig(player));
        update(player);
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

    public void update(ServerPlayerEntity player) {
        if (CombatControlNetworking.isListening(player)) {
            updateModdedPlayer(player);
        } else {
            updateVanillaPlayer(player);
        }
    }

    private void updateVanillaPlayer(ServerPlayerEntity player) {
        CombatConfig config = getConfig(player);

        // adjust the attack speed for vanilla players so that they know there is no cooldown
        EntityAttributeInstance attackSpeed = player.getAttributeInstance(EntityAttributes.ATTACK_SPEED);

        if (attackSpeed != null) {
            double value = config.isAttackCooldown() ? EntityAttributes.ATTACK_SPEED.value().getDefaultValue() : 1024;
            attackSpeed.setBaseValue(value);
        }
    }

    private void updateModdedPlayer(ServerPlayerEntity player) {
        CombatConfig config = getConfig(player);
        CombatAbilities abilities = getAbilities(player);
        boolean changed = false;

        {
            boolean b = config.isAttackCooldown();

            if (b != abilities.attackCooldown) {
                abilities.attackCooldown = b;
                changed = true;
            }
        } {
            boolean b = config.isAttackWhileUsing();

            if (b != abilities.attackWhileUsing) {
                abilities.attackWhileUsing = b;
                changed = true;
            }
        } {
            boolean b = config.isRenderSwingArmWhileUsing();

            if (b != abilities.renderArmSwingWhileUsing) {
                abilities.renderArmSwingWhileUsing = b;
                changed = true;
            }
        } {
            boolean b = config.isNoReequipWhenUsing();

            if (b != abilities.noReequipWhenUsing) {
                abilities.noReequipWhenUsing = b;
                changed = true;
            }
        }

        if (!changed) return;

        var packet = new CombatAbilitiesS2CPacket(abilities);
        ServerPlayNetworking.send(player, packet);
    }

    @Override
    public void copyData(ServerPlayerEntity source, ServerPlayerEntity target) {
        CombatConfig config = ((CombatControlPlayer) source).combatControl$getConfig();

        ((CombatControlPlayer) target).combatControl$setConfig(config);

        if (source.networkHandler != target.networkHandler) {
            update(target);
        }
    }
}
