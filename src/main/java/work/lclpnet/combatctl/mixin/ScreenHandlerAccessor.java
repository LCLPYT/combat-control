package work.lclpnet.combatctl.mixin;

import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerSyncHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ScreenHandler.class)
public interface ScreenHandlerAccessor {

    @Accessor
    ScreenHandlerSyncHandler getSyncHandler();
}
