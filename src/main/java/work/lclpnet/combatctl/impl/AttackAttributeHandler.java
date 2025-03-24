package work.lclpnet.combatctl.impl;

import com.google.common.collect.ImmutableMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import work.lclpnet.combatctl.type.ToolInfo;
import work.lclpnet.combatctl.type.ToolInfoCapture;
import work.lclpnet.combatctl.type.ToolType;

import java.util.Map;

/**
 * @implNote This implementation is taken from GoldenAgeCombat and remapped into yarn mappings.
 */
public class AttackAttributeHandler {

    private static final Map<ToolType, Double> ATTACK_DAMAGE_BONUS_OVERRIDES = ImmutableMap.of(
            ToolType.SWORD, 3.0,
            ToolType.AXE, 2.0,
            ToolType.PICKAXE, 1.0,
            ToolType.SHOVEL, 0.0,
            ToolType.HOE, 0.0
    );

    @ApiStatus.Internal
    public static void _modifyAttackDamageAttribute(ItemStack stack) {
        if (StaticCombatControl.get().globalConfig().isModernDamageValues()) return;

        // don't change items whose attributes have already been changed via component
        if (attackDamageModified(stack)) return;

        setClassicAttackDamage(stack);
    }

    public static void setClassicAttackDamage(ItemStack stack) {
        ToolInfo info = ((ToolInfoCapture) stack.getItem()).combatControl$getToolInfo();

        if (info == null) return;

        double newValue = ATTACK_DAMAGE_BONUS_OVERRIDES.getOrDefault(info.type(), Double.NaN);

        if (Double.isNaN(newValue)) return;

        if (info.type() != ToolType.HOE) {
            newValue += info.material().attackDamageBonus();
        }

        AttributeModifiersComponent component = stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
        AttributeModifiersComponent modified = modifyComponent(component, newValue);
        stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, modified);
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
