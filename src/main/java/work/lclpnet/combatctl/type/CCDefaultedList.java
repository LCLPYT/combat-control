package work.lclpnet.combatctl.type;

import java.util.function.Consumer;

public interface CCDefaultedList {

    <T> void combatControl$setOnAcquire(Consumer<T> onAcquire);
}
