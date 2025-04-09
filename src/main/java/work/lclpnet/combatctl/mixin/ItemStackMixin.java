package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import work.lclpnet.combatctl.impl.DynamicItemHandler;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @WrapOperation(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/Item;use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"
            )
    )
    private ActionResult combatControl$prioritizeOffHandBlocking(Item instance, World world, PlayerEntity user, Hand hand, Operation<ActionResult> original) {
        ItemStack self = (ItemStack) (Object) this;

        // if the player blocks with a combat-control-handled sword the main hand, give priority to the off-hand item if it can block
        if (hand == Hand.OFF_HAND
                || user.getOffHandStack().getOrDefault(DataComponentTypes.BLOCKS_ATTACKS, null) == null
                || DynamicItemHandler.getInstance().unhandled(self, DynamicItemHandler.Property.SWORD_BLOCKING)) {
            return original.call(instance, world, user, hand);
        }

        return ActionResult.PASS;
    }
}
