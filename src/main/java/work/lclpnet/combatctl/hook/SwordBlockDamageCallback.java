package work.lclpnet.combatctl.hook;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

/**
 * Called when a player is about to block damage using sword blocking.
 * Can be used to adjust the damage reduction feature or inhibit it entirely.
 */
public interface SwordBlockDamageCallback {

    Hook<SwordBlockDamageCallback> HOOK = HookFactory.createArrayBacked(SwordBlockDamageCallback.class, hooks -> (player, source, originalDamage, currentDamage, stack) -> {
        for (var hook : hooks) {
            currentDamage = hook.calculateDamage(player, source, originalDamage, currentDamage, stack);
        }

        return currentDamage;
    });

    /**
     * Calculates the resulting damage that the player blocking should take.
     * @param player The player.
     * @param source The damage source.
     * @param originalDamage The original damage, without any previous calculations.
     * @param currentDamage The damage that the player is bound to receive currently. This may include previous calculations based on the original damage.
     * @param stack The item used for blocking.
     * @return The new calculated damage the player should take. Other calculations may still change this value.
     */
    float calculateDamage(ServerPlayer player, DamageSource source, float originalDamage, float currentDamage, ItemStack stack);
}
