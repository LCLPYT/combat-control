package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.config.PlayerConfig;
import work.lclpnet.combatctl.type.ToolInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow public abstract Item getItem();

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
