package work.lclpnet.combatctl.api;

import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

public interface CombatDetail<T extends Enum<T>> {

    T getValue(CombatStyle style);

    static <T extends Enum<T>> CombatDetail<T> of(Function<CombatStyle, T> mapper) {
        return mapper::apply;
    }

    @ApiStatus.Experimental
    static <T extends Enum<T>> CombatDetail<T> of(T modern, T old) {
        return style -> style == CombatStyle.OLD ? old : modern;
    }
}
