package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import work.lclpnet.combatctl.impl.DynamicItemHandler;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @WrapOperation(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/Item;use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult combatControl$prioritizeOffHandBlocking(Item instance, Level world, Player user, InteractionHand hand, Operation<InteractionResult> original) {
        ItemStack self = (ItemStack) (Object) this;

        // if the player blocks with a combat-control-handled sword the main hand, give priority to the off-hand item if it can block
        if (hand == InteractionHand.OFF_HAND
                || user.getOffhandItem().getOrDefault(DataComponents.BLOCKS_ATTACKS, null) == null
                || DynamicItemHandler.getInstance().unhandled(self, DynamicItemHandler.Property.SWORD_BLOCKING)) {
            return original.call(instance, world, user, hand);
        }

        return InteractionResult.PASS;
    }
}
