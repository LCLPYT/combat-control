package work.lclpnet.combatctl.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A small helper class with a few utility methods for handling attribute related lines on an item tooltip.
 * @implNote This implementation is taken from GoldenAgeCombat and remapped into yarn mappings.
 */
public final class AttributeTooltipHelper {

    private AttributeTooltipHelper() {

    }

    /**
     * Collect all {@link EntityAttributeModifier}s on an {@link ItemStack} into a map separated by {@link EquipmentSlot}.
     *
     * @param stack the item stack
     * @return the map
     */
    public static Map<EquipmentSlot, Multimap<EntityAttribute, EntityAttributeModifier>> getAttributesBySlot(ItemStack stack) {
        Map<EquipmentSlot, Multimap<EntityAttribute, EntityAttributeModifier>> map = Maps.newLinkedHashMap();

        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            Multimap<EntityAttribute, EntityAttributeModifier> multimap = stack.getAttributeModifiers(equipmentSlot);
            if (!multimap.isEmpty()) map.put(equipmentSlot, multimap);
        }
        return map;
    }

    /**
     * Tests if a component describes a given attribute and potential modifier.
     *
     * @param component         the component to compare to the provided attribute values
     * @param attribute         the attribute to compare to
     * @param attributeModifier a potential attribute modifier in case checking for a specific modifier is desired
     * @return does the component describe the given attribute and potential modifier
     */
    public static boolean matchesAttributeComponent(Text component, EntityAttribute attribute, @Nullable EntityAttributeModifier attributeModifier) {
        TranslatableTextContent translatableContents = null;
        if (component.getContent() instanceof TranslatableTextContent translatableContents1) {
            translatableContents = translatableContents1;
        } else if (component instanceof MutableText mutableComponent && !mutableComponent.getSiblings().isEmpty() && mutableComponent.getSiblings().get(0).getContent() instanceof TranslatableTextContent translatableContents1) {
            translatableContents = translatableContents1;
        }
        if (translatableContents != null) {
            double scaledAmount = 0.0;
            String translationKey = null;
            if (attributeModifier != null) {
                scaledAmount = getScaledAttributeAmount(attribute, attributeModifier);
                if (attributeModifier.getValue() > 0.0D) {
                    translationKey = "attribute.modifier.plus." + attributeModifier.getOperation().getId();
                } else if (attributeModifier.getValue() < 0.0D) {
                    scaledAmount *= -1.0D;
                    translationKey = "attribute.modifier.take." + attributeModifier.getOperation().getId();
                }
            }
            Object[] args = translatableContents.getArgs();
            if ((attributeModifier == null || translationKey != null && translatableContents.getKey().equals(translationKey)) && args.length >= 2) {
                if (attributeModifier == null || args[0].equals(ItemStack.MODIFIER_FORMAT.format(scaledAmount))) {
                    if (args[1] instanceof Text component1 && component1.getContent() instanceof TranslatableTextContent translatableComponent1) {
                        return translatableComponent1.getKey().equals(attribute.getTranslationKey());
                    }
                }
            }
        }
        return false;
    }

    /**
     * Adjusts a given value for an attribute modifier for the tooltip just like vanilla does it.
     *
     * @param attribute         the attribute the value is for
     * @param attributeModifier the modifier where the value comes from
     * @return the adjusted value, potentially still the original input
     */
    private static double getScaledAttributeAmount(EntityAttribute attribute, EntityAttributeModifier attributeModifier) {
        // apply same scaling to attribute value as is done by vanilla for the tooltip
        double attributeAmount = attributeModifier.getValue();
        if (attributeModifier.getOperation() != EntityAttributeModifier.Operation.MULTIPLY_BASE && attributeModifier.getOperation() != EntityAttributeModifier.Operation.MULTIPLY_TOTAL) {
            if (attribute.equals(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)) {
                return attributeAmount * 10.0D;
            } else {
                return attributeAmount;
            }
        } else {
            return attributeAmount * 100.0D;
        }
    }

    /**
     * Remove all tooltip lines related to attributes, as indicated by {@link #findAttributesStart(List)} and {@link #findAttributesEnd(List)}.
     *
     * @param lines tooltip lines to edit
     * @return the start index from where on attribute lines have been removed in the original tooltip
     */
    public static int removeAllAttributes(List<Text> lines) {
        int startIndex = findAttributesStart(lines);
        if (startIndex >= 0) {
            int endIndex = findAttributesEnd(lines);
            if (startIndex < endIndex) {
                // remove start to end, both inclusive, therefore +1
                for (int i = 0; i < endIndex - startIndex + 1; i++) {
                    lines.remove(startIndex);
                }
                // return start index when removal was successful for further processing
                return startIndex;
            }
        }
        return -1;
    }

    /**
     * Finds the index of the first attributes related line which is usually a blank line, otherwise returns <code>-1</code>.
     *
     * @param lines tooltip lines to analyze
     * @return the found index
     */
    public static int findAttributesStart(List<Text> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).getContent() instanceof TranslatableTextContent contents && contents.getKey().startsWith("item.modifiers.")) {
                // attributes have a blank line above, we try to include that
                if (--i >= 0 && lines.get(i).getContent() == PlainTextContent.EMPTY) {
                    return i;
                } else {
                    return ++i;
                }
            }
        }
        return -1;
    }

    /**
     * Finds the index of the last attributes related line, otherwise returns <code>-1</code>.
     *
     * @param lines tooltip lines to analyze
     * @return the found index
     */
    public static int findAttributesEnd(List<Text> lines) {
        int index = -1;
        for (int i = 0; i < lines.size(); i++) {
            final Text component = lines.get(i);
            TranslatableTextContent translatableComponent = null;
            if (component.getContent() instanceof TranslatableTextContent translatableComponent1) {
                translatableComponent = translatableComponent1;
            } else if (component.getContent() instanceof PlainTextContent textComponent && textComponent.string().equals(" ")) {
                if (!component.getSiblings().isEmpty() && component.getSiblings().get(0).getContent() instanceof TranslatableTextContent translatableComponent1) {
                    translatableComponent = translatableComponent1;
                }
            }
            if (translatableComponent != null && translatableComponent.getKey().startsWith("attribute.modifier.")) {
                index = i;
            }
        }
        return index;
    }

    /**
     * Calculates the total value of an {@link EntityAttribute} from a list of {@link EntityAttributeModifier}s.
     * <p>This method is copied from net.minecraft.entity.attribute.EntityAttributeInstance#computeValue().
     *
     * @param player    a player to get a base value from, otherwise <code>0.0</code> is used
     * @param attribute the attribute to calculate
     * @param modifiers all modifiers for that attribute
     * @return the total attribute value
     */
    public static double calculateAttributeValue(@Nullable PlayerEntity player, EntityAttribute attribute, Collection<EntityAttributeModifier> modifiers) {

        double baseValue = player != null ? player.getAttributeBaseValue(attribute) : 0.0;
        Map<EntityAttributeModifier.Operation, List<EntityAttributeModifier>> modifiersByOperation = modifiers.stream().collect(Collectors.groupingBy(EntityAttributeModifier::getOperation));

        for (EntityAttributeModifier attributeModifier : modifiersByOperation.getOrDefault(EntityAttributeModifier.Operation.ADDITION, List.of())) {
            baseValue += attributeModifier.getValue();
        }

        double multipliedValue = baseValue;

        for (EntityAttributeModifier attributeModifier : modifiersByOperation.getOrDefault(EntityAttributeModifier.Operation.MULTIPLY_BASE, List.of())) {
            multipliedValue += baseValue * attributeModifier.getValue();
        }

        for (EntityAttributeModifier attributeModifier : modifiersByOperation.getOrDefault(EntityAttributeModifier.Operation.MULTIPLY_TOTAL, List.of())) {
            multipliedValue *= 1.0D + attributeModifier.getValue();
        }

        return attribute.clamp(multipliedValue);
    }
}
