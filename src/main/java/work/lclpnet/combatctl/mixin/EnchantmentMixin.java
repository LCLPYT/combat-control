package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.item.enchantment.effects.AddValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
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
    private final List<ConditionalEffect<EnchantmentValueEffect>> oldSharpnessEffect = List.of(
            new ConditionalEffect<>(
                    // 1.25 * level
                    new AddValue(LevelBasedValue.perLevel(1.25f)),
                    // no requirements
                    Optional.empty()
            )
    );

    @WrapOperation(
            method = "modifyDamageFilteredValue(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/server/level/ServerLevel;ILnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lorg/apache/commons/lang3/mutable/MutableFloat;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/Enchantment;getEffects(Lnet/minecraft/core/component/DataComponentType;)Ljava/util/List;"
            )
    )
    public List<ConditionalEffect<EnchantmentValueEffect>> combatControl$modifySharpnessEffect(
            Enchantment instance, DataComponentType<List<ConditionalEffect<EnchantmentValueEffect>>> type,
            Operation<List<ConditionalEffect<EnchantmentValueEffect>>> original,
            @Local(argsOnly = true) ServerLevel world,
            @Local(argsOnly = true) DamageSource damageSource
    ) {
        // filter for player user
        if (!(damageSource.getEntity() instanceof ServerPlayer player)) {
            return original.call(instance, type);
        }

        // filter for damage component type
        if (!EnchantmentEffectComponents.DAMAGE.equals(type)) {
            return original.call(instance, type);
        }

        // filter user with old sharpness
        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        if (config.isModernSharpness()) {
            return original.call(instance, type);
        }

        // filter for sharpness enchantment
        var registry = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Enchantment self = (Enchantment) (Object) this;

        if (registry.getValue(Enchantments.SHARPNESS) != self) {
            return original.call(instance, type);
        }

        return oldSharpnessEffect;
    }
}
