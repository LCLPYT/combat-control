package work.lclpnet.combatctl.impl;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class EmptyDataComponentGetter implements DataComponentGetter {

    private EmptyDataComponentGetter() {}

    @Override
    public @Nullable <T> T get(@NonNull DataComponentType<? extends T> type) {
        return null;
    }

    public static EmptyDataComponentGetter getInstance() {
        return Holder.instance;
    }

    private static class Holder {
        private static final EmptyDataComponentGetter instance = new EmptyDataComponentGetter();
    }
}
