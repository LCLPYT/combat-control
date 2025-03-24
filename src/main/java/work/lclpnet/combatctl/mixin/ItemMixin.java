package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.type.ToolInfo;
import work.lclpnet.combatctl.type.ToolInfoCapture;
import work.lclpnet.combatctl.type.ToolType;

@Mixin(Item.class)
public class ItemMixin implements ToolInfoCapture {

    @Unique @Nullable private ToolInfo toolInfo = null;

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    public void combatControl$initToolInfo(Item.Settings settings, CallbackInfo ci) {
        toolInfo = ((ToolInfoCapture) settings).combatControl$getToolInfo();
    }

    @WrapOperation(
            method = "postMine",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;damage(ILnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;)V"
            )
    )
    public void combatControl$damageTool(ItemStack instance, int amount, LivingEntity miner, EquipmentSlot slot, Operation<Void> original) {
        if (!(miner instanceof ServerPlayerEntity player)) return;

        if (toolInfo == null || toolInfo.type() != ToolType.SWORD || CombatControl.get(player.getServer()).playerConfig(player).isModernItemDurability()) {
            original.call(instance, amount, miner, slot);
        } else {
            original.call(instance, 1, miner, slot);
        }
    }

    @Override
    public @Nullable ToolInfo combatControl$getToolInfo() {
        return toolInfo;
    }
}
