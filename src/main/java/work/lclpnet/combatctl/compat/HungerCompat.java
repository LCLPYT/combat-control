package work.lclpnet.combatctl.compat;

import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface HungerCompat {

    boolean onHungerLevelChange(Player player, int from, int to);

    boolean onSaturationChange(Player player, float from, float to);

    boolean onExhaustionChange(Player player, float from, float to);
}
