package work.lclpnet.combatctl.impl;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
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

import static net.minecraft.core.component.DataComponents.*;
import static net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND;
import static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;
import static net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE;
import static net.minecraft.world.item.Item.BASE_ATTACK_DAMAGE_ID;

public class DynamicItemHandler {

    private static final MapCodec<State> STATE_CODEC = State.CODEC.fieldOf(CCModInit.identifier("state").toString());

    private static final Map<ToolType, Double> ATTACK_DAMAGE_BONUS_OVERRIDES = ImmutableMap.of(
            ToolType.SWORD, 3.0,
            ToolType.AXE, 2.0,
            ToolType.PICKAXE, 1.0,
            ToolType.SHOVEL, 0.0,
            ToolType.HOE, 0.0
    );

    private static final Consumable CLASSIC_ENCHANTED_GOLDEN_APPLE = Consumables.defaultFood()
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                    new MobEffectInstance(MobEffects.REGENERATION, 600, 4),
                    new MobEffectInstance(MobEffects.RESISTANCE, 6000, 0),
                    new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0),
                    new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0)
            )))
            .build();

    private DynamicItemHandler() {
    }

    public void update(ServerPlayer player) {
        for (ItemStack stack : player.getInventory()) {
            adjustStackFor(stack, player);
        }
    }

    /**
     * Adjusts a given item stack for a given player, according to their config.
     * This method modifies item stack components, lore etc.
     *
     * @param stack  The item stack to modify.
     * @param player The player to modify the item stack for.
     */
    public void adjustStackFor(ItemStack stack, ServerPlayer player) {
        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);
        ToolInfo info = ToolInfo.of(stack).orElse(null);

        if (info != null) {
            adjustAttackDamage(config, info, stack);

            if (info.isSword()) {
                adjustBlocking(config, stack);
                adjustSwordDurabilityDamage(config, stack);
            } else {
                adjustToolDurabilityDamage(config, stack);
            }
        }

        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            adjustNotchApple(config, stack);
        }
    }

    private void adjustBlocking(PlayerConfig config, ItemStack stack) {
        if (config.isSwordBlocking()) {
            // don't replace existing blocking component - e.g. from other mods
            if (componentChanged(BLOCKS_ATTACKS, stack)) return;

            // no damage reduction here, as the 1.7.10 formula doesn't fit into base + dmg * factor
            // reduction is applied in PlayerEntityMixin::combatControl$modifySwordBlockingDamage instead
            var damageReductions = List.<BlocksAttacks.DamageReduction>of();
            var itemDamage = new BlocksAttacks.ItemDamageFunction(0, 0, 0);
            var component = new BlocksAttacks(0.f, 1.f, damageReductions, itemDamage,
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

            var component = stack.getOrDefault(ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
            var modifier = new AttributeModifier(BASE_ATTACK_DAMAGE_ID, newValue, ADD_VALUE);

            Optional<Double> originalDamage = component.modifiers().stream()
                    .filter(entry -> entry.matches(ATTACK_DAMAGE, BASE_ATTACK_DAMAGE_ID))
                    .findAny()
                    .map(ItemAttributeModifiers.Entry::modifier)
                    .map(AttributeModifier::amount);

            stack.set(ATTRIBUTE_MODIFIERS, component.withModifierAdded(ATTACK_DAMAGE, modifier, MAINHAND));
            editState(stack, state -> state.withHandled(Property.ATTACK_DAMAGE).withOriginalDamage(originalDamage));
        } else {
            if (unhandled(stack, Property.ATTACK_DAMAGE)) return;

            getState(stack).originalDamage().ifPresent(originalDamage -> {
                var component = stack.getOrDefault(ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
                var modifier = new AttributeModifier(BASE_ATTACK_DAMAGE_ID, originalDamage, ADD_VALUE);

                stack.set(ATTRIBUTE_MODIFIERS, component.withModifierAdded(ATTACK_DAMAGE, modifier, MAINHAND));
            });

            unsetHandled(stack, Property.ATTACK_DAMAGE);
        }
    }

    private void adjustSwordDurabilityDamage(PlayerConfig config, ItemStack stack) {
        if (!config.isModernItemDurability()) {
            if (componentChanged(TOOL, stack, c -> c.damagePerBlock() != 2)) return;

            Tool tool = stack.get(TOOL);

            if (tool == null) return;

            var component = new Tool(tool.rules(), tool.defaultMiningSpeed(), 1, tool.canDestroyBlocksInCreative());

            stack.set(TOOL, component);
            setHandled(stack, Property.DURABILITY_DAMAGE);
        } else {
            if (unhandled(stack, Property.DURABILITY_DAMAGE)) return;

            Tool tool = stack.get(TOOL);

            if (tool == null) return;

            var component = new Tool(tool.rules(), tool.defaultMiningSpeed(), 2, tool.canDestroyBlocksInCreative());

            stack.set(TOOL, component);
            unsetHandled(stack, Property.DURABILITY_DAMAGE);
        }
    }

    private void adjustToolDurabilityDamage(PlayerConfig config, ItemStack stack) {
        if (!config.isModernItemDurability()) {
            if (componentChanged(WEAPON, stack, c -> c.itemDamagePerAttack() != 2)) return;

            Weapon weapon = stack.get(WEAPON);

            if (weapon == null) return;

            var component = new Weapon(1, weapon.disableBlockingForSeconds());

            stack.set(WEAPON, component);
            setHandled(stack, Property.DURABILITY_DAMAGE);
        } else {
            if (unhandled(stack, Property.DURABILITY_DAMAGE)) return;

            Weapon weapon = stack.get(WEAPON);

            if (weapon == null) return;

            var component = new Weapon(2, weapon.disableBlockingForSeconds());

            stack.set(WEAPON, component);
            unsetHandled(stack, Property.DURABILITY_DAMAGE);
        }
    }

    private void adjustNotchApple(PlayerConfig config, ItemStack stack) {
        if (!config.isModernNotchApple()) {
            if (componentChanged(CONSUMABLE, stack, c -> !c.equals(Consumables.ENCHANTED_GOLDEN_APPLE))) return;

            stack.set(CONSUMABLE, CLASSIC_ENCHANTED_GOLDEN_APPLE);
            setHandled(stack, Property.NOTCH_APPLE);
        } else {
            if (unhandled(stack, Property.NOTCH_APPLE)) return;

            stack.set(CONSUMABLE, Consumables.ENCHANTED_GOLDEN_APPLE);
            unsetHandled(stack, Property.NOTCH_APPLE);
        }
    }

    private <T> boolean componentChanged(DataComponentType<T> type, ItemStack stack) {
        return componentChanged(type, stack, _ -> true);
    }

    private <T> boolean componentChanged(DataComponentType<T> type, ItemStack stack, Predicate<T> predicate) {
        var component = stack.getComponentsPatch().get(EmptyDataComponentGetter.getInstance(), type);

        return component != null && predicate.test(component);
    }

    private boolean attackDamagedChanged(ItemAttributeModifiers component) {
        for (var modifier : component.modifiers()) {
            if (modifier.attribute() == ATTACK_DAMAGE) {
                return true;
            }
        }

        return false;
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
        CustomData customData = stack.getOrDefault(CUSTOM_DATA, CustomData.EMPTY);

        return STATE_CODEC.codec().decode(NbtOps.INSTANCE, customData.copyTag())
                .resultOrPartial()
                .map(Pair::getFirst)
                .orElse(State.DEFAULT);
    }

    private void setState(ItemStack stack, State state) {
        CustomData customData = stack.getOrDefault(CUSTOM_DATA, CustomData.EMPTY);

        STATE_CODEC.codec().encode(state, NbtOps.INSTANCE, customData.copyTag())
                .resultOrPartial(error -> CCModInit.LOGGER.error("Failed to encode dynamic item state: {}", error))
                .ifPresent(nbt -> CustomData.set(CUSTOM_DATA, stack, (CompoundTag) nbt));
    }

    public boolean unhandled(ItemStack stack, Property property) {
        return !getHandled(stack).contains(property);
    }

    public static DynamicItemHandler getInstance() {
        return Holder.instance;
    }

    public enum Property {
        SWORD_BLOCKING,
        ATTACK_DAMAGE,
        DURABILITY_DAMAGE,
        NOTCH_APPLE
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
