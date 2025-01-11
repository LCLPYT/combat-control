package work.lclpnet.combatctl.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration of client-sided combat-control features.
 * Class is also available in server compile scope in order to be serialized into the same config file as the server-sided configuration.
 * Data if this class is only serialized on the client though.
 */
@Getter @Setter
public class ClientConfig {
}
