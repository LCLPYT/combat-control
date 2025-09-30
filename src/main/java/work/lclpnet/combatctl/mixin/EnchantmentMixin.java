package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.component.ComponentType;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelBasedValue;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.effect.EnchantmentEffectEntry;
import net.minecraft.enchantment.effect.EnchantmentValueEffect;
import net.minecraft.enchantment.effect.value.AddEnchantmentEffect;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.config.PlayerConfig;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("UnreachableCode")
@Mixin(Enchantment.class)
public class EnchantmentMixin {

    @Unique
    private final List<EnchantmentEffectEntry<EnchantmentValueEffect>> oldSharpnessEffect = List.of(
            new EnchantmentEffectEntry<>(
                    // 1.25 * level
                    new AddEnchantmentEffect(EnchantmentLevelBasedValue.linear(1.25f)),
                    // no requirements
                    Optional.empty()
            )
    );

    @WrapOperation(
            method = "modifyValue(Lnet/minecraft/component/ComponentType;Lnet/minecraft/server/world/ServerWorld;ILnet/minecraft/item/ItemStack;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;Lorg/apache/commons/lang3/mutable/MutableFloat;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/enchantment/Enchantment;getEffect(Lnet/minecraft/component/ComponentType;)Ljava/util/List;"
            )
    )
    public List<EnchantmentEffectEntry<EnchantmentValueEffect>> combatControl$modifySharpnessEffect(
            Enchantment instance, ComponentType<List<EnchantmentEffectEntry<EnchantmentValueEffect>>> type,
            Operation<List<EnchantmentEffectEntry<EnchantmentValueEffect>>> original,
            @Local(argsOnly = true) ServerWorld world,
            @Local(argsOnly = true) DamageSource damageSource
    ) {
        // filter for player user
        if (!(damageSource.getAttacker() instanceof ServerPlayerEntity player)) {
            return original.call(instance, type);
        }

        // filter for damage component type
        if (!EnchantmentEffectComponentTypes.DAMAGE.equals(type)) {
            return original.call(instance, type);
        }

        // filter user with old sharpness
        PlayerConfig config = CombatControl.get(player.getEntityWorld().getServer()).playerConfig(player);

        if (config.isModernSharpness()) {
            return original.call(instance, type);
        }

        // filter for sharpness enchantment
        var registry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        Enchantment self = (Enchantment) (Object) this;

        if (registry.get(Enchantments.SHARPNESS) != self) {
            return original.call(instance, type);
        }

        return oldSharpnessEffect;
    }
}
