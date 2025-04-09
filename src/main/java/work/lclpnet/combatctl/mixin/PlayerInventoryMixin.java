package work.lclpnet.combatctl.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import work.lclpnet.combatctl.impl.DynamicItemHandler;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {

    @Shadow @Final public PlayerEntity player;

    @ModifyVariable(
            method = {
                    "setSelectedStack",
                    "swapStackWithHotbar",
                    "insertStack(ILnet/minecraft/item/ItemStack;)Z",
                    "setStack"
            },
            at = @At("HEAD"),
            argsOnly = true
    )
    private ItemStack combatControl$modifyIncomingStack(ItemStack stack) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            DynamicItemHandler.getInstance().adjustStackFor(stack, serverPlayer);
        }

        return stack;
    }
}
