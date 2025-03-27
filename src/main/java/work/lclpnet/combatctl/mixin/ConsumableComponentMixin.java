package work.lclpnet.combatctl.mixin;

import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.config.PlayerConfig;

@Mixin(ConsumableComponent.class)
public class ConsumableComponentMixin {

    @Inject(
            method = "finishConsumption",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;emitGameEvent(Lnet/minecraft/registry/entry/RegistryEntry;)V"
            )
    )
    public void combatControl$afterApplyEffects(World world, LivingEntity user, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        // TODO this should be handled by dynamically overriding the stack components in the future
        if (world.isClient
                || !(user instanceof ServerPlayerEntity player)
                || !stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) return;

        PlayerConfig config = CombatControl.get(player.getServer()).playerConfig(player);

        if (config.isModernNotchApple()) return;

        player.removeStatusEffect(StatusEffects.ABSORPTION);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 600, 4));
    }
}
