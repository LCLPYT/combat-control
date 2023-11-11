package work.lclpnet.combatctl;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.api.CombatStyle;

public class CombatControlTest implements ModInitializer {

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("combatTest")
                    .requires(s -> s.hasPermissionLevel(2))
                    .then(CommandManager.literal("modern").executes(ctx -> {
                        MinecraftServer server = ctx.getSource().getServer();
                        CombatControl.get(server).setStyle(CombatStyle.MODERN);
                        return 1;
                    }))
                    .then(CommandManager.literal("old").executes(ctx -> {
                        MinecraftServer server = ctx.getSource().getServer();
                        CombatControl.get(server).setStyle(CombatStyle.OLD);
                        return 1;
                    })));
        });

        CombatControlMod.LOGGER.info("Test mod loaded");
    }
}
