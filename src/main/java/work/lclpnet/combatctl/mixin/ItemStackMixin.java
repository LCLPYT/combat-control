package work.lclpnet.combatctl.mixin;

import net.minecraft.component.ComponentMapImpl;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.event.AttributeModifierTooltipCallback;
import work.lclpnet.combatctl.impl.AttackAttributeHandler;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(
            method = "<init>(Lnet/minecraft/item/ItemConvertible;ILnet/minecraft/component/ComponentMapImpl;)V",
            at = @At("TAIL")
    )
    public void combatControl$applyAttributeModifiers(ItemConvertible item, int count, ComponentMapImpl components, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;

        AttackAttributeHandler.modifyAttackDamageAttribute(self);
    }

    @Inject(
            method = "appendAttributeModifierTooltip",
            at = @At("HEAD"),
            cancellable = true
    )
    public void combatControl$appendAttributeModifierTooltip(Consumer<Text> textConsumer, @Nullable PlayerEntity player, RegistryEntry<EntityAttribute> attribute, EntityAttributeModifier modifier, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;

        if (!AttributeModifierTooltipCallback.EVENT.invoker().shouldAppend(self, player, attribute, modifier)) {
            ci.cancel();
        }
    }
}
