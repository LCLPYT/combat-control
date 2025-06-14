package work.lclpnet.combatctl.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import work.lclpnet.combatctl.api.CombatControlClient;
import work.lclpnet.combatctl.impl.ItemStackTextConsumer;

import java.util.function.Consumer;

@Mixin(AttributeModifiersComponent.Display.Default.class)
public class AttributeModifiersComponent$Display$DefaultMixin {

    // combatControl$addModifierTooltip is originally taken from GoldenAgeCombat
    @ModifyVariable(method = "addTooltip", at = @At("LOAD"), ordinal = 0)
    private boolean combatControl$addModifierTooltip(boolean baseId) {
        // block the green tooltip formatting style for legacy type
        return baseId && !CombatControlClient.get().config().isOldAttributeStyle();
    }

    @WrapOperation(
            method = "addTooltip",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;getAttributeBaseValue(Lnet/minecraft/registry/entry/RegistryEntry;)D",
                    ordinal = 0
            )
    )
    private double combatControl$addSharpnessDamage(PlayerEntity instance, RegistryEntry<?> registryEntry, Operation<Double> original,
                                                    @Local(argsOnly = true) Consumer<Text> textConsumer) {

        double base = original.call(instance, registryEntry);

        if (!(textConsumer instanceof ItemStackTextConsumer consumer)) {
            return base;
        }

        ItemStack stack = consumer.stack();

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
}
