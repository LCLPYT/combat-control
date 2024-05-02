package work.lclpnet.combatctl.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.Nullable;

public interface AttributeModifierTooltipCallback {

    Event<AttributeModifierTooltipCallback> EVENT = EventFactory.createArrayBacked(AttributeModifierTooltipCallback.class, callbacks -> (stack, player, attribute, modifier) -> {
        for (AttributeModifierTooltipCallback callback : callbacks) {
            if (!callback.shouldAppend(stack, player, attribute, modifier)) return false;
        }

        return true;
    });

    boolean shouldAppend(ItemStack stack, @Nullable PlayerEntity player, RegistryEntry<EntityAttribute> attribute, EntityAttributeModifier modifier);
}
