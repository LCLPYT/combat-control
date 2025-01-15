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
    private boolean oldAttributeStyle = false;
}
