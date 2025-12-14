package work.lclpnet.combatctl.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import work.lclpnet.combatctl.api.CombatControlClient;
import work.lclpnet.combatctl.impl.ItemStackTextConsumer;

import java.util.function.Consumer;

@Mixin(ItemAttributeModifiers.Display.Default.class)
public class ItemAttributeModifiers$Display$DefaultMixin {

    // combatControl$addModifierTooltip is originally taken from GoldenAgeCombat
    @ModifyVariable(method = "apply", at = @At("LOAD"), ordinal = 0)
    private boolean combatControl$addModifierTooltip(boolean baseId) {
        // block the green tooltip formatting style for legacy type
        return baseId && !CombatControlClient.get().config().isOldAttributeStyle();
    }

    @WrapOperation(
            method = "apply",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getAttributeBaseValue(Lnet/minecraft/core/Holder;)D",
                    ordinal = 0
            )
    )
    private double combatControl$addSharpnessDamage(Player instance, Holder<?> registryEntry, Operation<Double> original,
                                                    @Local(argsOnly = true) Consumer<Component> textConsumer) {

        double base = original.call(instance, registryEntry);

        if (!(textConsumer instanceof ItemStackTextConsumer consumer)) {
            return base;
        }

        ItemStack stack = consumer.stack();

        var component = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        var sharpness = component.entrySet()
                .stream()
                .filter(entry -> entry.getKey().is(Enchantments.SHARPNESS))
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
}
