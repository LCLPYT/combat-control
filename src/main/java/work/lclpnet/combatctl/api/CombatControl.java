package work.lclpnet.combatctl.api;

import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.combatctl.impl.CombatControlImpl;

public interface CombatControl {

    void setDefaultStyle(CombatStyle style);

    <T extends Enum<T>> void setDefaultDetail(CombatDetail<T> detail, @NotNull T value);

    <T extends Enum<T>> T getDefaultDetail(CombatDetail<T> detail);

    void setStyle(ServerPlayerEntity player, @NotNull CombatStyle style);

    <T extends Enum<T>> void setDetail(ServerPlayerEntity player, CombatDetail<T> detail, @NotNull T value);

    <T extends Enum<T>> T getDetail(ServerPlayerEntity player,  CombatDetail<T> detail);

    static CombatControl getInstance() {
        return CombatControlImpl.getInstance();
    }
}