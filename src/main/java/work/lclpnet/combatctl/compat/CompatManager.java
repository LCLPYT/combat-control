package work.lclpnet.combatctl.compat;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class CompatManager {

    private final HungerCompat hungerCompat;

    private CompatManager() {
        this.hungerCompat = createHungerCompat();
    }

    public HungerCompat getHungerCompat() {
        return hungerCompat;
    }

    private HungerCompat createHungerCompat() {
        ClassLoader classLoader = getClass().getClassLoader();

        try {
            Class.forName("work.lclpnet.kibu.hook.player.PlayerFoodHooks", false, classLoader);

            // kibu-hooks is present
            return new KibuHungerCompat();
        } catch (ClassNotFoundException e) {
            return new VanillaHungerCompat();
        }
    }

    public static CompatManager get() {
        return Holder.instance;
    }

    private static class Holder {
        private static final CompatManager instance = new CompatManager();
    }
}
