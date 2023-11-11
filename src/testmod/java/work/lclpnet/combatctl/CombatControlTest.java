package work.lclpnet.combatctl;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.api.CombatStyle;

public class CombatControlTest implements ModInitializer {

    @Override
    public void onInitialize() {
        CombatControl control = CombatControl.getInstance();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("combatTest")
                    .requires(s -> s.hasPermissionLevel(2))
                    .then(CommandManager.literal("modern").executes(ctx -> {
                        control.setDefaultStyle(CombatStyle.MODERN);
                        return 1;
                    }))
                    .then(CommandManager.literal("old").executes(ctx -> {
                        control.setDefaultStyle(CombatStyle.OLD);
                        return 1;
                    })));
        });

        CombatControlMod.LOGGER.info("Test mod loaded");
    }
}
