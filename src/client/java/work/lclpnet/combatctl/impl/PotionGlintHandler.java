package work.lclpnet.combatctl.impl;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import work.lclpnet.combatctl.api.CombatControlClient;

public class PotionGlintHandler {

    private PotionGlintHandler() {}

    public static boolean shouldHaveGlint(ItemStack stack) {
        if (!CombatControlClient.get().config().isPotionGlint() || !(stack.getItem() instanceof PotionItem)) {
            return false;
        }

        Boolean override = stack.getOrDefault(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, null);

        if (override != null) {
            return false;
        }

        var potions = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);

        return potions.hasEffects();
    }
}
