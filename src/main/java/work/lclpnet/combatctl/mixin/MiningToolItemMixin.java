package work.lclpnet.combatctl.mixin;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.config.PlayerConfig;
import work.lclpnet.combatctl.type.ToolMaterialCapture;

@Mixin(MiningToolItem.class)
public class MiningToolItemMixin implements ToolMaterialCapture {

    @Unique private ToolMaterial toolMaterial = null;

    @Override
    public ToolMaterial combatControl$getToolMaterial() {
        return toolMaterial;
    }

    @Override
    public void combatControl$setToolMaterial(ToolMaterial toolMaterial) {
        this.toolMaterial = toolMaterial;
    }

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void combatControl$captureToolMaterial(ToolMaterial material, TagKey<?> effectiveBlocks, float attackDamage, float attackSpeed, Item.Settings settings, CallbackInfo ci) {
        this.toolMaterial = material;
    }

    // combatControl$postHit is taken from GoldenAgeCombat
    @Inject(
            method = "postHit",
            at = @At("HEAD"),
            cancellable = true
    )
    public void combatControl$postHit(ItemStack stack, LivingEntity target, LivingEntity attacker, CallbackInfoReturnable<Boolean> cir) {
        if (!(attacker instanceof ServerPlayerEntity player)) return;

        PlayerConfig config = CombatControl.get(player.getServer()).playerConfig(player);

        if (config.isModernItemDurability()) return;

        stack.damage(1, attacker, EquipmentSlot.MAINHAND);

        cir.setReturnValue(true);
    }
}
