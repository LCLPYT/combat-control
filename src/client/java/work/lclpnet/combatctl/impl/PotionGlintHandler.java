package work.lclpnet.combatctl.impl;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import work.lclpnet.combatctl.api.CombatControlClient;

public class PotionGlintHandler {

    private PotionGlintHandler() {}

    public static boolean shouldHaveGlint(ItemStack stack) {
        if (!CombatControlClient.get().config().isPotionGlint() || !(stack.getItem() instanceof PotionItem)) {
            return false;
        }

        Boolean override = stack.getOrDefault(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, null);

        if (override != null) {
            return false;
        }

        var potions = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

        return potions.hasEffects();
    }
}
