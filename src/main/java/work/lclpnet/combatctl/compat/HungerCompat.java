package work.lclpnet.combatctl.compat;

import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface HungerCompat {

    boolean onHungerLevelChange(PlayerEntity player, int from, int to);

    boolean onSaturationChange(PlayerEntity player, float from, float to);

    boolean onExhaustionChange(PlayerEntity player, float from, float to);
}
