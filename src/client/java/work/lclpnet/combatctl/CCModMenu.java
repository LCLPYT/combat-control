package work.lclpnet.combatctl;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import work.lclpnet.combatctl.config.ConfigScreenBuilder;

public class CCModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        var configManager = CCModInit.configManager().orElse(null);

        return configManager != null
                ? new ConfigScreenBuilder(configManager)
                : ModMenuApi.super.getModConfigScreenFactory();
    }
}
