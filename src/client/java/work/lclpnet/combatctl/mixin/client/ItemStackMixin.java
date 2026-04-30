package work.lclpnet.combatctl.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
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
    @WrapMethod(method = "addAttributeTooltips")
    private void combatControl$wrapAppendAttributeModifiersTooltip(Consumer<Component> consumer, TooltipDisplay display, @Nullable Player player, Operation<Void> original,
                                                                   @Share("modifierSlots") LocalRef<Set<EquipmentSlotGroup>> ref) {

        if (!CombatControlClient.get().config().isOldAttributeStyle()) {
            original.call(consumer, display, player);
            return;
        }

        ref.set(EnumSet.noneOf(EquipmentSlotGroup.class));

        List<Component> tooltipLines = new ArrayList<>();

        // we replace the component consumer with our own list, so we can later perform actions on all attribute lines
        // without having to filter them from all tooltip lines
        var stack = (ItemStack) (Object) this;
        original.call(new ItemStackTextConsumer(stack, tooltipLines::add), display, player);

        // this removes the equipment slot group lines when there are only attributes for a single group,
        // like attack damage and speed for the main hand
        if (this.allMatchSameEquipmentSlot(ref.get())) {
            // remove all equipment slot group lines as well as the empty line above
            tooltipLines.removeIf((Component component) -> {
                if (component == CommonComponents.EMPTY) {
                    return true;
                }

                TextColor color = component.getStyle().getColor();
                return color != null && color.serialize().equals(ChatFormatting.GRAY.getName());
            });

            // add back one single empty line above all attributes
            if (!tooltipLines.isEmpty()) {
                tooltipLines.addFirst(CommonComponents.EMPTY);
            }
        }

        tooltipLines.forEach(consumer);
    }

    // combatControl$appendAttributeModifiersTooltip is originally taken from GoldenAgeCombat
    @ModifyArg(method = "addAttributeTooltips", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V"))
    private TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> combatControl$appendAttributeModifiersTooltip(
            EquipmentSlotGroup modifierSlot, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> action,
            @Share("modifierSlots") LocalRef<Set<EquipmentSlotGroup>> ref) {

        return (attr, modifier, display) -> {
            if (!CombatControlClient.get().abilities().attackCooldown && Attributes.ATTACK_SPEED.equals(attr))
                return;

            action.accept(attr, modifier, display);

            Set<EquipmentSlotGroup> slots = ref.get();

            if (slots != null) {
                slots.add(modifierSlot);
            }
        };
    }

    // allMatchSameEquipmentSlot is taken from GoldenAgeCombat
    @Unique
    private boolean allMatchSameEquipmentSlot(Collection<EquipmentSlotGroup> modifierSlots) {
        // test if there is an equipment slot that all groups match,
        // e.g. groups main hand, any, hand all match slot main hand
        return !modifierSlots.isEmpty() && Arrays.stream(EquipmentSlot.values())
                .anyMatch(slot -> modifierSlots.stream()
                        .allMatch(modifierSlot -> modifierSlot.test(slot)));
    }

    // the attribute modifier display component does not have the item stack context
    // therefore, inject a custom text consumer that has the current stack context
    @Inject(
            method = "addDetailsToTooltip",
            at = @At("HEAD")
    )
    private void combatControl$injectStackTextConsumer(Item.TooltipContext context, TooltipDisplay displayComponent, @Nullable Player player, TooltipFlag type, Consumer<Component> textConsumer, CallbackInfo ci,
                                                       @Local(argsOnly = true, name = "builder") LocalRef<Consumer<Component>> textConsumerRef) {

        var stack = (ItemStack) (Object) this;

        textConsumerRef.set(new ItemStackTextConsumer(stack, textConsumer));
    }

    @Inject(
            method = "hasFoil",
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
