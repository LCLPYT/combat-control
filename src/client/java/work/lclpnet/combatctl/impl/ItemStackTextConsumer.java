package work.lclpnet.combatctl.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public record ItemStackTextConsumer(ItemStack stack, Consumer<Component> delegate) implements Consumer<Component> {

    @Override
    public void accept(Component text) {
        delegate.accept(text);
    }
}
