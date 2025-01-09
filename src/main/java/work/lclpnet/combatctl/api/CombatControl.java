package work.lclpnet.combatctl.api;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import work.lclpnet.combatctl.config.GlobalConfig;
import work.lclpnet.combatctl.config.PlayerConfig;
import work.lclpnet.combatctl.type.CombatControlServer;

import java.util.function.Consumer;

public interface CombatControl {

    /**
     * Configures the global {@link PlayerConfig} that is persisted in the configuration file. Then automatically calls {@link #update()}.
     * The configuration is also applied to every online player.
     * @param action A consumer that configures the global player config and all the configs of online players.
     */
    void configurePlayers(Consumer<PlayerConfig> action);

    /**
     * Configures a single player's {@link PlayerConfig}, then automatically calls {@link #update(ServerPlayerEntity)} on that player.
     * @param player The player to modify.
     * @param action A consumer that configures that player's config.
     */
    void configurePlayer(ServerPlayerEntity player, Consumer<PlayerConfig> action);

    /**
     * Gets the {@link GlobalConfig} that controls features that don't involve specific players.
     * If you modify the config, make sure to call {@link #update()} afterward, or use {@link #configureGlobal(Consumer)}.
     * @return The {@link GlobalConfig} persisted in the configuration file.
     */
    GlobalConfig globalConfig();

    /**
     * Gets the global {@link PlayerConfig} that is persisted in the configuration file.
     * If you modify the config, make sure to call {@link #update()} afterward, or use {@link #configurePlayers(Consumer)}.
     * @return The {@link PlayerConfig} persisted in the configuration file
     */
    PlayerConfig playerConfig();

    /**
     * Gets the {@link PlayerConfig} of a player.
     * If you modify the config, make sure to call {@link #update(ServerPlayerEntity)} afterward for that player, or use {@link #configurePlayer(ServerPlayerEntity, Consumer)}.
     * @param player The player.
     * @return The player's config.
     */
    PlayerConfig playerConfig(ServerPlayerEntity player);

    /**
     * Updates all players and synchronizes them with their associated {@link PlayerConfig}s.
     */
    void update();

    /**
     * Synchronizes a player with their {@link PlayerConfig}.
     * This involves sending a packet to clients with combat-control installed, if needed.
     * This method should also be called for vanilla players, as they need updates to attributes etc. as well without the mod.
     * @param player The player to update.
     */
    void update(ServerPlayerEntity player);

    /**
     * Copies one players config to another.
     * Then updates the other player, if needed.
     */
    void copyData(ServerPlayerEntity source, ServerPlayerEntity target);

    /**
     * Configures the {@link GlobalConfig}, then automatically calls {@link #update()}.
     * @param action A consumer that configures the global config.
     */
    default void configureGlobal(Consumer<GlobalConfig> action) {
        action.accept(globalConfig());
        update();
    }

    /**
     * Set and apply a {@link CombatStyle} for everyone.
     * @param style The combat style.
     */
    default void setStyle(CombatStyle style) {
        configureGlobal(style::configure);
        configurePlayers(style::configure);
    }

    /**
     * Sets a {@link CombatStyle} for a single player.
     * @param player The player.
     * @param style The combat style.
     */
    default void setStyle(ServerPlayerEntity player, CombatStyle style) {
        configurePlayer(player, style::configure);
    }

    /**
     * Get the CombatControl instance of a {@link MinecraftServer}.
     * Please note: this should only be called after {@link net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents#SERVER_STARTING} is done, otherwise this method may return null.
     * @param server The server instance.
     * @return The CombatControl instance.
     */
    static CombatControl get(MinecraftServer server) {
        return ((CombatControlServer) server).combatControl$get();
    }
}