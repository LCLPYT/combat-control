package work.lclpnet.combatctl.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.impl.CombatConfig;

@Mixin(SwordItem.class)
public class SwordItemMixin {

    // combatControl$postMine is taken from GoldenAgeCombat
    @Inject(
            method = "postMine",
            at = @At("HEAD"),
            cancellable = true
    )
    public void combatControl$postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner, CallbackInfoReturnable<Boolean> cir) {
        if (!(miner instanceof ServerPlayerEntity player)) return;

        CombatConfig config = CombatControl.get(player.getServer()).getConfig(player);

        if (config.isModernItemDurability()) return;

        if (state.getHardness(world, pos) != 0.0F) {
            stack.damage(1, miner, (livingEntity) -> {
                livingEntity.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND);
            });
        }

        cir.setReturnValue(true);
    }
}
