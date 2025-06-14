package work.lclpnet.combatctl.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControlClient;
import work.lclpnet.combatctl.impl.ItemStackTextConsumer;
import work.lclpnet.combatctl.impl.PotionGlintHandler;

import java.util.*;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    // combatControl$wrapAppendAttributeModifiersTooltip is originally taken from GoldenAgeCombat
    @WrapMethod(method = "appendAttributeModifiersTooltip")
    private void combatControl$wrapAppendAttributeModifiersTooltip(Consumer<Text> textConsumer, TooltipDisplayComponent displayComponent, @Nullable PlayerEntity player, Operation<Void> original,
                                                                   @Share("modifierSlots") LocalRef<Set<AttributeModifierSlot>> ref) {

        if (!CombatControlClient.get().config().isOldAttributeStyle()) {
            original.call(textConsumer, displayComponent, player);
            return;
        }

        ref.set(EnumSet.noneOf(AttributeModifierSlot.class));

        List<Text> tooltipLines = new ArrayList<>();

        // we replace the component consumer with our own list, so we can later perform actions on all attribute lines
        // without having to filter them from all tooltip lines
        var stack = (ItemStack) (Object) this;
        original.call(new ItemStackTextConsumer(stack, tooltipLines::add), displayComponent, player);

        // this removes the equipment slot group lines when there are only attributes for a single group,
        // like attack damage and speed for the main hand
        if (this.allMatchSameEquipmentSlot(ref.get())) {
            // remove all equipment slot group lines as well as the empty line above
            tooltipLines.removeIf((Text component) -> {
                if (component == ScreenTexts.EMPTY) {
                    return true;
                }

                TextColor color = component.getStyle().getColor();
                return color != null && color.getName().equals(Formatting.GRAY.getName());
            });

            // add back one single empty line above all attributes
            if (!tooltipLines.isEmpty()) {
                tooltipLines.addFirst(ScreenTexts.EMPTY);
            }
        }

        tooltipLines.forEach(textConsumer);
    }

    // combatControl$appendAttributeModifiersTooltip is originally taken from GoldenAgeCombat
    @ModifyArg(method = "appendAttributeModifiersTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;applyAttributeModifier(Lnet/minecraft/component/type/AttributeModifierSlot;Lorg/apache/commons/lang3/function/TriConsumer;)V"))
    private TriConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier, AttributeModifiersComponent.Display> combatControl$appendAttributeModifiersTooltip(
            AttributeModifierSlot modifierSlot, TriConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier, AttributeModifiersComponent.Display> action,
            @Share("modifierSlots") LocalRef<Set<AttributeModifierSlot>> ref) {

        return (attr, modifier, display) -> {
            if (!CombatControlClient.get().abilities().attackCooldown && EntityAttributes.ATTACK_SPEED.equals(attr))
                return;

            action.accept(attr, modifier, display);

            Set<AttributeModifierSlot> slots = ref.get();

            if (slots != null) {
                slots.add(modifierSlot);
            }
        };
    }

    // allMatchSameEquipmentSlot is taken from GoldenAgeCombat
    @Unique
    private boolean allMatchSameEquipmentSlot(Collection<AttributeModifierSlot> modifierSlots) {
        // test if there is an equipment slot that all groups match,
        // e.g. groups main hand, any, hand all match slot main hand
        return !modifierSlots.isEmpty() && Arrays.stream(EquipmentSlot.values())
                .anyMatch(slot -> modifierSlots.stream()
                        .allMatch(modifierSlot -> modifierSlot.matches(slot)));
    }

    // the attribute modifier display component does not have the item stack context
    // therefore, inject a custom text consumer that has the current stack context
    @Inject(
            method = "appendTooltip",
            at = @At("HEAD")
    )
    private void combatControl$injectStackTextConsumer(Item.TooltipContext context, TooltipDisplayComponent displayComponent, @Nullable PlayerEntity player, TooltipType type, Consumer<Text> textConsumer, CallbackInfo ci,
                                                       @Local(argsOnly = true) LocalRef<Consumer<Text>> textConsumerRef) {

        var stack = (ItemStack) (Object) this;

        textConsumerRef.set(new ItemStackTextConsumer(stack, textConsumer));
    }

    @Inject(
            method = "hasGlint",
            at = @At("RETURN"),
            cancellable = true
    )
    private void combatControl$overrideEnchantmentGlint(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;

        if (PotionGlintHandler.shouldHaveGlint((ItemStack) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
