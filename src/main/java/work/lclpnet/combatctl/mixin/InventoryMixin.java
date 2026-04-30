package work.lclpnet.combatctl.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import work.lclpnet.combatctl.impl.DynamicItemHandler;

@Mixin(Inventory.class)
public class InventoryMixin {

    @Shadow @Final public Player player;

    @ModifyVariable(
            method = {
                    "setSelectedItem",
                    "addAndPickItem",
                    "add(ILnet/minecraft/world/item/ItemStack;)Z",
                    "setItem"
            },
            at = @At("HEAD"),
            argsOnly = true,
            name = "itemStack"
    )
    private ItemStack combatControl$modifyIncomingStack(ItemStack itemStack) {
        if (player instanceof ServerPlayer serverPlayer) {
            DynamicItemHandler.getInstance().adjustStackFor(itemStack, serverPlayer);
        }

        return itemStack;
    }
}
