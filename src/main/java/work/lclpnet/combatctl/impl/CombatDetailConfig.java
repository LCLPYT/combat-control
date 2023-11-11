package work.lclpnet.combatctl.impl;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.combatctl.api.CombatDetail;
import work.lclpnet.combatctl.api.CombatStyle;

import java.util.Map;
import java.util.Objects;

/**
 * An immutable configuration for {@link CombatDetail}.
 */
@ApiStatus.Internal
public class CombatDetailConfig {

    private final Map<CombatDetail<?>, Object> details;

    public CombatDetailConfig(Map<CombatDetail<?>, Object> details) {
        this.details = details;
    }

    public <T extends Enum<T>> CombatDetailConfig with(CombatDetail<T> detail, @NotNull T value) {
        Objects.requireNonNull(detail, "Detail must not be null");
        Objects.requireNonNull(value, "Value must not be null");

        var copy = new Object2ObjectAVLTreeMap<>(details);
        copy.put(detail, value);

        return new CombatDetailConfig(copy);
    }

    @NotNull
    public static CombatDetailConfig getConfig(@NotNull CombatStyle style) {
        Objects.requireNonNull(style);

        return switch (style) {
            case MODERN -> Modern.instance;
            case OLD -> Old.instance;
        };
    }

    private static CombatDetailConfig createConfig(CombatStyle style) {
        Map<CombatDetail<?>, Object> details = new Object2ObjectAVLTreeMap<>();

        for (CombatDetail<?> detail : CombatDetails.getDetails()) {
            details.put(detail, detail.getValue(style));
        }

        return new CombatDetailConfig(details);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends Enum<T>> T get(CombatDetail<T> detail) {
        return (T) details.get(detail);
    }

    // lazy singletons
    private static class Modern {
        private static final CombatDetailConfig instance = createConfig(CombatStyle.MODERN);
    }

    private static class Old {
        private static final CombatDetailConfig instance = createConfig(CombatStyle.OLD);
    }
}
