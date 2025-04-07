package work.lclpnet.combatctl.impl;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.combatctl.CCModInit;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.config.PlayerConfig;
import work.lclpnet.combatctl.type.ToolInfo;
import work.lclpnet.combatctl.type.ToolType;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static net.minecraft.component.DataComponentTypes.*;
import static net.minecraft.component.type.AttributeModifierSlot.MAINHAND;
import static net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE;
import static net.minecraft.entity.attribute.EntityAttributes.ATTACK_DAMAGE;
import static net.minecraft.item.Item.BASE_ATTACK_DAMAGE_MODIFIER_ID;

public class DynamicItemHandler {

    private static final MapCodec<State> STATE_CODEC = State.CODEC.fieldOf(CCModInit.identifier("state").toString());
    private static final Map<ToolType, Double> ATTACK_DAMAGE_BONUS_OVERRIDES = ImmutableMap.of(
            ToolType.SWORD, 3.0,
            ToolType.AXE, 2.0,
            ToolType.PICKAXE, 1.0,
            ToolType.SHOVEL, 0.0,
            ToolType.HOE, 0.0
    );

    private DynamicItemHandler() {}

    private boolean attackDamagedChanged(AttributeModifiersComponent component) {
        for (var modifier : component.modifiers()) {
            if (modifier.attribute() == ATTACK_DAMAGE) {
                return true;
            }
        }

        return false;
    }

    public void update(ServerPlayerEntity player) {
        for (ItemStack stack : player.getInventory()) {
            adjustStackFor(stack, player);
        }
    }

    /**
     * Adjusts a given item stack for a given player, according to their config.
     * This method modifies item stack components, lore etc.
     * @param stack The item stack to modify.
     * @param player The player to modify the item stack for.
     */
    public void adjustStackFor(ItemStack stack, ServerPlayerEntity player) {
        PlayerConfig config = CombatControl.get(player.getServer()).playerConfig(player);
        ToolInfo info = ToolInfo.of(stack).orElse(null);

        if (info != null) {
            applyBlocking(config, info, stack);
            adjustAttackDamage(config, info, stack);
        }
    }

    private void applyBlocking(PlayerConfig config, ToolInfo info, ItemStack stack) {
        if (!info.isSword()) return;

        if (config.isSwordBlocking()) {
            // don't replace existing blocking component - e.g. from other mods
            if (componentChanged(BLOCKS_ATTACKS, stack)) return;

            // no damage reduction here, as the 1.7.10 formula doesn't fit into base + dmg * factor
            // reduction is applied in PlayerEntityMixin::combatControl$modifySwordBlockingDamage instead
            var damageReductions = List.<BlocksAttacksComponent.DamageReduction>of();
            var itemDamage = new BlocksAttacksComponent.ItemDamage(0, 0, 0);
            var component = new BlocksAttacksComponent(0.f, 1.f, damageReductions, itemDamage,
                    Optional.empty(), Optional.empty(), Optional.empty());

            stack.set(BLOCKS_ATTACKS, component);
            setHandled(stack, Property.SWORD_BLOCKING);
        } else {
            // only remove the blocking component if it was added by combat-control
            if (unhandled(stack, Property.SWORD_BLOCKING)) return;

            stack.remove(BLOCKS_ATTACKS);
            unsetHandled(stack, Property.SWORD_BLOCKING);
        }
    }

    private void adjustAttackDamage(PlayerConfig config, ToolInfo info, ItemStack stack) {
        if (!config.isModernDamageValues()) {
            if (componentChanged(ATTRIBUTE_MODIFIERS, stack, this::attackDamagedChanged)) return;

            double newValue = ATTACK_DAMAGE_BONUS_OVERRIDES.getOrDefault(info.type(), Double.NaN);

            if (Double.isNaN(newValue)) return;

            if (info.type() != ToolType.HOE) {
                newValue += info.material().attackDamageBonus();
            }

            var component = stack.getOrDefault(ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
            var modifier = new EntityAttributeModifier(BASE_ATTACK_DAMAGE_MODIFIER_ID, newValue, ADD_VALUE);

            Optional<Double> originalDamage = component.modifiers().stream()
                    .filter(entry -> entry.matches(ATTACK_DAMAGE, BASE_ATTACK_DAMAGE_MODIFIER_ID))
                    .findAny()
                    .map(AttributeModifiersComponent.Entry::modifier)
                    .map(EntityAttributeModifier::value);

            stack.set(ATTRIBUTE_MODIFIERS, component.with(ATTACK_DAMAGE, modifier, MAINHAND));
            editState(stack, state -> state.withHandled(Property.ATTACK_DAMAGE).withOriginalDamage(originalDamage));
        } else {
            if (unhandled(stack, Property.ATTACK_DAMAGE)) return;

            getState(stack).originalDamage().ifPresent(originalDamage -> {
                var component = stack.getOrDefault(ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
                var modifier = new EntityAttributeModifier(BASE_ATTACK_DAMAGE_MODIFIER_ID, originalDamage, ADD_VALUE);

                stack.set(ATTRIBUTE_MODIFIERS, component.with(ATTACK_DAMAGE, modifier, MAINHAND));
            });

            unsetHandled(stack, Property.ATTACK_DAMAGE);
        }
    }

    private <T> boolean componentChanged(ComponentType<T> type, ItemStack stack) {
        return componentChanged(type, stack, t -> true);
    }
    
    private <T> boolean componentChanged(ComponentType<T> type, ItemStack stack, Predicate<T> predicate) {
        var optComponent = stack.getComponentChanges().get(type);

        return optComponent != null && optComponent.isPresent() && predicate.test(optComponent.get());
    }

    private void setHandled(ItemStack stack, Property property) {
        editState(stack, state -> state.withHandled(property));
    }

    private void unsetHandled(ItemStack stack, Property property) {
        editState(stack, state -> state.withoutHandled(property));
    }

    private @NotNull Set<Property> getHandled(ItemStack stack) {
        return getState(stack).handled();
    }

    private void editState(ItemStack stack, UnaryOperator<State> modifier) {
        setState(stack, modifier.apply(getState(stack)));
    }

    private State getState(ItemStack stack) {
        NbtComponent customData = stack.getOrDefault(CUSTOM_DATA, NbtComponent.DEFAULT);

        return customData.get(STATE_CODEC).resultOrPartial().orElse(State.DEFAULT);
    }

    private void setState(ItemStack stack, State state) {
        NbtComponent customData = stack.getOrDefault(CUSTOM_DATA, NbtComponent.DEFAULT);

        customData.with(NbtOps.INSTANCE, STATE_CODEC, state)
                .result()
                .ifPresent(customDataNew -> stack.set(CUSTOM_DATA, customDataNew));
    }

    public boolean unhandled(ItemStack stack, Property property) {
        return !getHandled(stack).contains(property);
    }

    public static DynamicItemHandler getInstance() {
        return Holder.instance;
    }

    public enum Property {
        SWORD_BLOCKING,
        ATTACK_DAMAGE
    }

    public record State(Set<Property> handled, Optional<Double> originalDamage) {

        private static final Codec<Property> PROPERTY_CODEC = Codec.STRING.xmap(
                s -> Property.valueOf(s.toUpperCase(Locale.ROOT)),
                prop -> prop.name().toLowerCase(Locale.ROOT)
        );

        private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PROPERTY_CODEC.listOf().xmap(Set::copyOf, List::copyOf).optionalFieldOf("handled", Set.of()).forGetter(State::handled),
                Codec.DOUBLE.optionalFieldOf("original_damage").forGetter(State::originalDamage)
        ).apply(instance, State::new));

        public static final State DEFAULT = new State(Set.of(), Optional.empty());

        public State withHandled(Property property) {
            if (property == null || handled.contains(property)) {
                return this;
            }

            Set<Property> handled = new HashSet<>(this.handled.size() + 1);
            handled.addAll(this.handled);
            handled.add(property);

            return new State(handled, originalDamage);
        }

        public State withoutHandled(Property property) {
            if (!this.handled.contains(property)) {
                return this;
            }

            var handled = this.handled.stream()
                    .filter(prop -> prop != property)
                    .collect(Collectors.toSet());

            return new State(handled, originalDamage);
        }

        public State withOriginalDamage(Optional<Double> originalDamage) {
            if (this.originalDamage.equals(originalDamage)) {
                return this;
            }

            return new State(handled, originalDamage);
        }
    }

    private static class Holder {
        private static final DynamicItemHandler instance = new DynamicItemHandler();
    }
}
