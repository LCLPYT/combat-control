package work.lclpnet.combatctl.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.combatctl.CCModInit;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.config.PlayerConfig;
import work.lclpnet.combatctl.type.ToolInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static net.minecraft.component.DataComponentTypes.BLOCKS_ATTACKS;
import static net.minecraft.component.DataComponentTypes.CUSTOM_DATA;

public class DynamicItemHandler {

    private static final Codec<Property> PROPERTY_CODEC = Codec.STRING.xmap(
            s -> Property.valueOf(s.toUpperCase(Locale.ROOT)),
            prop -> prop.name().toLowerCase(Locale.ROOT)
    );
    private static final MapCodec<List<Property>> HANDLED_CODEC = PROPERTY_CODEC.listOf().fieldOf(CCModInit.MOD_ID);

    private DynamicItemHandler() {}

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
            if (!isHandled(stack, Property.SWORD_BLOCKING)) return;

            stack.remove(BLOCKS_ATTACKS);
            unsetHandled(stack, Property.SWORD_BLOCKING);
        }
    }

    private boolean componentChanged(ComponentType<?> type, ItemStack stack) {
        var optComponent = stack.getComponentChanges().get(type);

        return optComponent != null && optComponent.isPresent();
    }

    private void setHandled(ItemStack stack, Property property) {
        if (property == null) return;

        NbtComponent customData = stack.getOrDefault(CUSTOM_DATA, NbtComponent.DEFAULT);
        List<Property> handled = getHandled(stack);

        if (handled.contains(property)) return;

        var handledNew = new ArrayList<Property>(handled.size() + 1);
        handledNew.addAll(handled);
        handledNew.add(property);

        customData.with(NbtOps.INSTANCE, HANDLED_CODEC, handledNew)
                .result()
                .ifPresent(customDataNew -> stack.set(CUSTOM_DATA, customDataNew));
    }

    private void unsetHandled(ItemStack stack, Property property) {
        if (property == null) return;

        NbtComponent customData = stack.getOrDefault(CUSTOM_DATA, NbtComponent.DEFAULT);
        List<Property> handled = getHandled(stack);

        if (!handled.contains(property)) return;

        var handledNew = handled.stream().filter(prop -> prop != property).toList();

        customData.with(NbtOps.INSTANCE, HANDLED_CODEC, handledNew)
                .result()
                .ifPresent(customDataNew -> stack.set(CUSTOM_DATA, customDataNew));
    }

    private @NotNull List<Property> getHandled(ItemStack stack) {
        NbtComponent customData = stack.getOrDefault(CUSTOM_DATA, NbtComponent.DEFAULT);

        return customData.get(HANDLED_CODEC).resultOrPartial().orElse(List.of());
    }

    public boolean isHandled(ItemStack stack, Property property) {
        return getHandled(stack).contains(property);
    }

    public static DynamicItemHandler getInstance() {
        return Holder.instance;
    }

    public enum Property {
        SWORD_BLOCKING
    }

    private static class Holder {
        private static final DynamicItemHandler instance = new DynamicItemHandler();
    }
}
