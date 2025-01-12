package work.lclpnet.combatctl.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeComment;
import com.electronwill.nightconfig.core.serde.annotations.SerdeSkipDeserializingIf;
import com.electronwill.nightconfig.core.serde.annotations.SerdeSkipSerializingIf;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class CombatControlConfig {

    @SerdeComment("Default player configuration")
    @SerdeSkipDeserializingIf(SerdeSkipDeserializingIf.SkipDeIf.IS_MISSING)
    public final PlayerConfig player = new PlayerConfig();

    @SerdeComment("Global configuration that doesn't involve specific players")
    @SerdeSkipDeserializingIf(SerdeSkipDeserializingIf.SkipDeIf.IS_MISSING)
    public final GlobalConfig global = new GlobalConfig();

    @SerdeComment("Client configuration")
    @SerdeSkipDeserializingIf(
            value = SerdeSkipDeserializingIf.SkipDeIf.CUSTOM,
            customCheck = "skipClientDeserializationOnServer"
    )
    @SerdeSkipSerializingIf(
            value = SerdeSkipSerializingIf.SkipSerIf.CUSTOM,
            customCheck = "skipClientSerializationOnServer"
    )
    public final ClientConfig client = new ClientConfig();

    private static boolean skipClientDeserializationOnServer(Object client) {
        return client == null || FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT;
    }

    private static boolean skipClientSerializationOnServer(ClientConfig ignoredClient) {
        return FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT;
    }
}
