package work.lclpnet.combatctl.mixin;

import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.type.CCDefaultedList;

import java.util.function.Consumer;

@Mixin(DefaultedList.class)
public class DefaultedListMixin implements CCDefaultedList {

    @Unique @Nullable
    private Consumer<Object> onAcquire = null;

    @SuppressWarnings("unchecked")
    @Override
    public <T> void combatControl$setOnAcquire(Consumer<T> onAcquire) {
        this.onAcquire = (Consumer<Object>) onAcquire;
    }

    @Inject(
            method = "set",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;set(ILjava/lang/Object;)Ljava/lang/Object;"
            )
    )
    public void combatControl$set(int index, Object element, CallbackInfoReturnable<Object> cir) {
        if (onAcquire != null) {
            onAcquire.accept(element);
        }
    }

    @Inject(
            method = "add",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(ILjava/lang/Object;)V"
            )
    )
    public void combatControl$add(int index, Object element, CallbackInfo ci) {
        if (onAcquire != null) {
            onAcquire.accept(element);
        }
    }
}
