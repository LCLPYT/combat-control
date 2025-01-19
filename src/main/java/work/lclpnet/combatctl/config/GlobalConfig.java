package work.lclpnet.combatctl.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeComment;
import lombok.Getter;
import lombok.Setter;

/**
 * A configuration for non-player specific combat details that is available within the server context.
 */
@Getter @Setter
public class GlobalConfig {

    @SerdeComment("If enabled, tools like axes will deal the modern amount of damage that takes cooldown into account. If disabled, damage values will be reverted / adapted to 1.8 and previous versions")
    private boolean modernDamageValues = true;

    @SerdeComment("Expand hitboxes by 10% to make hits more accurate")
    private boolean largerHitboxes = false;

    @SerdeComment("Period in ticks when to refresh player pings")
    private int pingUpdateTicks = 5;

    @SerdeComment("Threshold in milliseconds when to consider the difference in players's ping an outlier (lag spike)")
    private double pingSpikeMs = 50.d;
}
