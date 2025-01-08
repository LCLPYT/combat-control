package work.lclpnet.combatctl.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeComment;
import com.electronwill.nightconfig.core.serde.annotations.SerdeSkipDeserializingIf;

public class CombatControlConfig {

    @SerdeComment("Default player configuration")
    @SerdeSkipDeserializingIf(SerdeSkipDeserializingIf.SkipDeIf.IS_MISSING)
    public final PlayerConfig player = new PlayerConfig();

    @SerdeComment("Global configuration that doesn't involve specific players")
    @SerdeSkipDeserializingIf(SerdeSkipDeserializingIf.SkipDeIf.IS_MISSING)
    public final GlobalConfig global = new GlobalConfig();
}
