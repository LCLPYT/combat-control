package work.lclpnet.combatctl.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeComment;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration of client-sided combat-control features.
 * Class is also available in server compile scope in order to be serialized into the same config file as the server-sided configuration.
 * Data if this class is only serialized on the client though.
 */
@Getter @Setter
public class ClientConfig {

    @SerdeComment("Disables the smooth sneak animation, like in older versions")
    private boolean instantEyeHeight = false;

    @SerdeComment("Displays attributes like in earlier versions. Recognizable by the blue text.")
    private boolean oldAttributeStyle = true;

    @SerdeComment("Renders the classic bobbing effect, with a slight head tilt when jumping. Will only work if view bobbing is enabled.")
    private boolean oldBobbing = false;

    @SerdeComment("Disables flashing effect of hearts when taking damage. This makes it easier to see your current health.")
    private boolean noFlashingHearts = true;

    @SerdeComment("Allow servers to temporarily override the old bobbing setting (recommended).")
    private boolean serverBobbingOverride = true;

    @SerdeComment("Puts an enchantment glint on potion items, like in 1.19.3 and earlier.")
    private boolean potionGlint = true;

    @SerdeComment("Use classic snowball particle")
    private boolean classicSnowballParticle = true;
}
