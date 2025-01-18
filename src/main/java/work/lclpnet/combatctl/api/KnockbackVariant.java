package work.lclpnet.combatctl.api;

import com.electronwill.nightconfig.core.serde.annotations.SerdeComment;

public enum KnockbackVariant {

    @SerdeComment("Does not adjust knockback behaviour")
    DEFAULT,

    @SerdeComment("Apply consistent upwards knockback with every hit. Used to be enabled on most 1.8 pvp servers.")
    NO_SCALING,

    @SerdeComment("Knockback will be adjusted depending on the player's latency to the server. Reduces the advantage of high ping players in pvp combos.")
    PING_ADJUSTED,

}
