package work.lclpnet.combatctl.util;

import lombok.Getter;

public class EnvUtil {

    @Getter
    private static final boolean client = detectClient();

    private static boolean detectClient() {
        try {
            Class.forName("net.minecraft.client.MinecraftClient", false, EnvUtil.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
