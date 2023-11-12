package work.lclpnet.combatctl.mixin;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.impl.CombatConfig;

@Mixin(MiningToolItem.class)
public class MiningToolItemMixin {

    // combatControl$postHit is taken from GoldenAgeCombat
    @Inject(
            method = "postHit",
            at = @At("HEAD"),
            cancellable = true
    )
    public void combatControl$postHit(ItemStack stack, LivingEntity target, LivingEntity attacker, CallbackInfoReturnable<Boolean> cir) {
        if (!(attacker instanceof ServerPlayerEntity player)) return;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(player);

        if (config.isModernItemDurability()) return;

        stack.damage(1, attacker, (livingEntity) -> {
            livingEntity.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND);
        });

        cir.setReturnValue(true);
    }
}
