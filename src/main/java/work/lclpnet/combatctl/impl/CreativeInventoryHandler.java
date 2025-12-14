package work.lclpnet.combatctl.impl;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import work.lclpnet.combatctl.mixin.AbstractContainerMenuAccessor;
import work.lclpnet.kibu.hook.player.PlayerInventoryHooks;

import java.util.HashSet;
import java.util.Set;

public class CreativeInventoryHandler {

    private final Set<Entry> entries = new HashSet<>();

    public void init() {
        // When putting items from the creative inventory into the player inventory, a de-sync can happen with dynamic items.
        // Therefore, sync the slot at the end of the server tick

        PlayerInventoryHooks.MODIFIED_CREATIVE_INVENTORY.register(event
                -> entries.add(new Entry(event.player().connection, event.slot())));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (entries.isEmpty()) return;

            for (var entry : entries) {
                ServerPlayer player = entry.handler.player;

                if (player == null) continue;

                AbstractContainerMenu screenHandler = player.inventoryMenu;

                if (screenHandler == null) continue;

                ContainerSynchronizer syncHandler = ((AbstractContainerMenuAccessor) screenHandler).getSynchronizer();
                Slot slot = screenHandler.getSlot(entry.slot);

                if (slot == null || slot.container != player.getInventory()) continue;

                ItemStack stack = player.getInventory().getItem(slot.getContainerSlot());

                syncHandler.sendSlotChange(screenHandler, entry.slot, stack);
            }

            entries.clear();
        });
    }

    private record Entry(ServerGamePacketListenerImpl handler, int slot) {}
}
