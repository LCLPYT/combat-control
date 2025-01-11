package work.lclpnet.combatctl.config;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public interface ConfigAccess<C> {

    @NotNull C config();

    void save();
}
