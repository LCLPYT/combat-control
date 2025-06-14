package work.lclpnet.combatctl.impl;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public record ItemStackTextConsumer(ItemStack stack, Consumer<Text> delegate) implements Consumer<Text> {

    @Override
    public void accept(Text text) {
        delegate.accept(text);
    }
}
