package work.lclpnet.combatctl.impl;

import com.google.common.collect.ImmutableMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import work.lclpnet.combatctl.api.GlobalCombatControl;
import work.lclpnet.combatctl.type.ToolMaterialCapture;

import java.util.Map;

/**
 * @implNote This implementation is taken from GoldenAgeCombat and remapped into yarn mappings.
 */
public class AttackAttributeHandler {
    private static final Map<Class<? extends Item>, Double> ATTACK_DAMAGE_BONUS_OVERRIDES = ImmutableMap.of(SwordItem.class, 3.0, AxeItem.class, 2.0, PickaxeItem.class, 1.0, ShovelItem.class, 0.0, HoeItem.class, 0.0);

    public static void modifyAttackDamageAttribute(ItemStack stack) {
        if (GlobalCombatControl.get().getGlobalConfig().isModernDamageValues()) return;

        for (Map.Entry<Class<? extends Item>, Double> entry : ATTACK_DAMAGE_BONUS_OVERRIDES.entrySet()) {
            if (!entry.getKey().isInstance(stack.getItem())) continue;

            // don't change items whose attributes have already been changed via component
            if (attackDamageModified(stack)) return;

            double newValue = entry.getValue();
            Item item = stack.getItem();

            if (!(item instanceof HoeItem) && item instanceof ToolMaterialCapture capture) {
                ToolMaterial material = capture.combatControl$getToolMaterial();
                newValue += material.attackDamageBonus();
            }

            AttributeModifiersComponent component = stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
            AttributeModifiersComponent modified = modifyComponent(component, newValue);
            stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, modified);
            break;
        }
    }

    private static boolean attackDamageModified(ItemStack stack) {
        var change = stack.getComponentChanges().get(DataComponentTypes.ATTRIBUTE_MODIFIERS);

        //noinspection OptionalAssignedToNull
        if (change == null || change.isEmpty()) return false;

        AttributeModifiersComponent component = change.get();

        for (var modifier : component.modifiers()) {
            if (modifier.attribute() == EntityAttributes.ATTACK_DAMAGE) {
                return true;
            }
        }

        return false;
    }

    private static AttributeModifiersComponent modifyComponent(AttributeModifiersComponent component, double newValue) {
        EntityAttributeModifier modifier = new EntityAttributeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID,
                newValue, EntityAttributeModifier.Operation.ADD_VALUE);

        return component.with(EntityAttributes.ATTACK_DAMAGE, modifier, AttributeModifierSlot.MAINHAND);
    }
}
