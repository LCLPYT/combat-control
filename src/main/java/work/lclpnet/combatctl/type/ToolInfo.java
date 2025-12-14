package work.lclpnet.combatctl.type;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;

import java.util.Optional;

public record ToolInfo(ToolType type, ToolMaterial material) {

    public boolean isSword() {
        return type == ToolType.SWORD;
    }

    public static Optional<ToolInfo> of(Item item) {
        return Optional.ofNullable(((ToolInfoCapture) item).combatControl$getToolInfo());
    }

    public static Optional<ToolInfo> of(ItemStack stack) {
        return of(stack.getItem());
    }
}
