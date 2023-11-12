package work.lclpnet.combatctl.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import work.lclpnet.combatctl.CombatControlMod;
import work.lclpnet.combatctl.api.GlobalCombatControl;
import work.lclpnet.combatctl.mixin.ItemAccessor;

import java.util.Map;
import java.util.UUID;

/**
 * @implNote This implementation is taken from GoldenAgeCombat and remapped into yarn mappings.
 */
public class AttackAttributeHandler {
    public static final UUID BASE_ATTACK_DAMAGE_UUID = ItemAccessor.combatControl$getAttackDamageModifierUUID();
    private static final String ATTACK_DAMAGE_MODIFIER_NAME = CombatControlMod.identifier("attack_damage_modifier").toString();
    private static final Map<Class<? extends ToolItem>, Double> ATTACK_DAMAGE_BONUS_OVERRIDES = ImmutableMap.of(SwordItem.class, 4.0, AxeItem.class, 3.0, PickaxeItem.class, 2.0, ShovelItem.class, 1.0, HoeItem.class, 0.0);

    public static void onItemAttributeModifiers(ItemStack stack, EquipmentSlot equipmentSlot, Multimap<EntityAttribute, EntityAttributeModifier> attributeModifiers) {
        if (GlobalCombatControl.get().getGlobalConfig().isModernDamageValues()) return;

        if (equipmentSlot != EquipmentSlot.MAINHAND) return;

        // don't change items whose attributes have already been changed via the nbt tag
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("AttributeModifiers", NbtElement.LIST_TYPE)) return;

        for (Map.Entry<Class<? extends ToolItem>, Double> entry : ATTACK_DAMAGE_BONUS_OVERRIDES.entrySet()) {
            if (entry.getKey().isInstance(stack.getItem())) {
                setNewAttributeValue(attributeModifiers, ((ToolItem) stack.getItem()).getMaterial().getAttackDamage() + entry.getValue());
                break;
            }
        }
    }

    private static void setNewAttributeValue(Multimap<EntityAttribute, EntityAttributeModifier> attributeModifiers, double newValue) {
        attributeModifiers.removeAll(EntityAttributes.GENERIC_ATTACK_DAMAGE);

        EntityAttributeModifier modifier = new EntityAttributeModifier(AttackAttributeHandler.BASE_ATTACK_DAMAGE_UUID,
                AttackAttributeHandler.ATTACK_DAMAGE_MODIFIER_NAME, newValue, EntityAttributeModifier.Operation.ADDITION);

        attributeModifiers.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, modifier);
    }
}
