package work.lclpnet.combatctl.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.combatctl.api.CombatControlClient;
import work.lclpnet.combatctl.impl.PotionGlintHandler;

import java.util.*;
import java.util.function.BiConsumer;
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
        original.call((Consumer<Text>) tooltipLines::add, displayComponent, player);

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
    @ModifyArg(method = "appendAttributeModifiersTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;applyAttributeModifier(Lnet/minecraft/component/type/AttributeModifierSlot;Ljava/util/function/BiConsumer;)V"))
    private BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> combatControl$appendAttributeModifiersTooltip(
            AttributeModifierSlot modifierSlot, BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> action,
            @Share("modifierSlots") LocalRef<Set<AttributeModifierSlot>> ref) {

        return (attr, modifier) -> {
            if (!CombatControlClient.get().abilities().attackCooldown && EntityAttributes.ATTACK_SPEED.equals(attr))
                return;

            action.accept(attr, modifier);

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

    // combatControl$addModifierTooltip is originally taken from GoldenAgeCombat
    @ModifyVariable(method = "appendAttributeModifierTooltip", at = @At("LOAD"), ordinal = 0)
    private boolean combatControl$addModifierTooltip(boolean baseId) {
        // block the green tooltip formatting style for legacy type
        return baseId && !CombatControlClient.get().config().isOldAttributeStyle();
    }

    @WrapOperation(
            method = "appendAttributeModifierTooltip",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;getAttributeBaseValue(Lnet/minecraft/registry/entry/RegistryEntry;)D",
                    ordinal = 0
            )
    )
    private double combatControl$addSharpnessDamage(PlayerEntity instance, RegistryEntry<?> registryEntry, Operation<Double> original) {
        double base = original.call(instance, registryEntry);
        ItemStack stack = (ItemStack) (Object) this;

        var component = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        var sharpness = component.getEnchantmentEntries()
                .stream()
                .filter(entry -> entry.getKey().matchesKey(Enchantments.SHARPNESS))
                .findAny()
                .orElse(null);

        if (sharpness == null) {
            return base;
        }

        int level = sharpness.getIntValue();

        // damage formula doesn't respect custom damage enchantment definitions from datapacks
        double bonusDamage = CombatControlClient.get().abilities().modernSharpness
                ? (1 + 0.5 * (level - 1))
                : (1.25 * level);

        return base + bonusDamage;
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
