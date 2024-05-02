package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import work.lclpnet.combatctl.api.CombatControl;

@Mixin(Item.class)
public class ItemMixin {

    @WrapOperation(
            method = "postMine",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;damage(ILnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;)V"
            )
    )
    public void combatControl$damageTool(ItemStack instance, int amount, LivingEntity miner, EquipmentSlot slot, Operation<Void> original) {
        if (miner instanceof ServerPlayerEntity player && instance.getItem() instanceof SwordItem
            && !CombatControl.get(player.getServer()).getConfig(player).isModernItemDurability()) {
            original.call(instance, 1, miner, slot);
        } else {
            original.call(instance, amount, miner, slot);
        }
    }
}
