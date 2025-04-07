package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import work.lclpnet.combatctl.impl.DynamicItemHandler;
import work.lclpnet.combatctl.type.CCDefaultedList;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/collection/DefaultedList;ofSize(ILjava/lang/Object;)Lnet/minecraft/util/collection/DefaultedList;"
            )
    )
    private DefaultedList<?> combatControl$wrapMain(int size, Object defaultValue, Operation<DefaultedList<?>> original,
                                                    @Local(argsOnly = true) PlayerEntity player) {

        DefaultedList<?> list = original.call(size, defaultValue);

        // register a callback for adjusting items in player inventories dynamically
        if (player instanceof ServerPlayerEntity serverPlayer) {
            var handler = DynamicItemHandler.getInstance();

            ((CCDefaultedList) list).<ItemStack>combatControl$setOnAcquire(stack -> handler.adjustStackFor(stack, serverPlayer));
        }

        return list;
    }
}
