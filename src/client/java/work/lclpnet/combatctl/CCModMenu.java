package work.lclpnet.combatctl;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import work.lclpnet.combatctl.config.ConfigScreenBuilder;

public class CCModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return ModMenuApi.super.getModConfigScreenFactory();
        }

        return parent -> CCModInit.configManager()
                .map(ConfigScreenBuilder::new)
                .map(builder -> builder.create(parent))
                .orElse(null);
    }
}
