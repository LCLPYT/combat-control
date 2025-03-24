package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.component.MergedComponentMap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.config.PlayerConfig;
import work.lclpnet.combatctl.impl.AttackAttributeHandler;
import work.lclpnet.combatctl.impl.SwordBlockingHandler;
import work.lclpnet.combatctl.type.ToolInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow public abstract Item getItem();

    @Inject(
            method = "<init>(Lnet/minecraft/item/ItemConvertible;ILnet/minecraft/component/MergedComponentMap;)V",
            at = @At("TAIL")
    )
    public void combatControl$applyAttributeModifiers(ItemConvertible item, int count, MergedComponentMap components, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;

        AttackAttributeHandler._modifyAttackDamageAttribute(self);
    }

    @WrapOperation(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/Item;use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"
            )
    )
    private ActionResult combatControl$use(Item instance, World world, PlayerEntity user, Hand hand, Operation<ActionResult> original) {
        ActionResult res = SwordBlockingHandler.useItem(instance, user, hand);

        if (res == ActionResult.PASS) {
            return original.call(instance, world, user, hand);
        }

        return res;
    }

    @WrapOperation(
            method = "onStoppedUsing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/Item;onStoppedUsing(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)Z"
            )
    )
    private boolean combatControl$onStoppedUsing(Item instance, ItemStack stack, World world, LivingEntity user, int remainingUseTicks, Operation<Boolean> original) {
        if (SwordBlockingHandler.stopUsing(instance, user)) {
            return true;
        }

        return original.call(instance, stack, world, user, remainingUseTicks);
    }

    @WrapMethod(method = "getMaxUseTime")
    private int combatControl$getMaxUseTime(LivingEntity user, Operation<Integer> original) {
        if (SwordBlockingHandler.canBlockWith(user, getItem())) {
            return 72000;
        }

        return original.call(user);
    }

    @WrapOperation(
            method = "postDamageEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;damage(ILnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;)V"
            )
    )
    private void combatControl$modifyPostDamageEntityDurability(ItemStack instance, int amount, LivingEntity entity, EquipmentSlot slot, Operation<Void> original) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            original.call(instance, amount, entity, slot);
            return;
        }

        PlayerConfig config = CombatControl.get(player.getServer()).playerConfig(player);

        if (config.isModernItemDurability()) {
            original.call(instance, amount, entity, slot);
            return;
        }

        amount = ToolInfo.of(instance)
                .filter(info -> !info.isSword())
                .isPresent() ? 1 : amount;

        original.call(instance, amount, entity, slot);
    }
}
