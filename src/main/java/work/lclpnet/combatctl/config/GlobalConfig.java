package work.lclpnet.combatctl.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeComment;
import lombok.Getter;
import lombok.Setter;

/**
 * A configuration for combat details that are not within a player context. i.e. item stack or entity context etc.
 */
@Getter @Setter
public class GlobalConfig {

    @SerdeComment("If enabled, tools like axes will deal the modern amount of damage that takes cooldown into account. If disabled, damage values will be reverted / adapted to 1.8 and previous versions")
    private boolean modernDamageValues = true;

    @SerdeComment("Expand hitboxes by 10% to make hits more accurate")
    private boolean largerHitboxes = false;

}
