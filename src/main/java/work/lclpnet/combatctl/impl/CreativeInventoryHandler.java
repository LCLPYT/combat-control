package work.lclpnet.combatctl.impl;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerSyncHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import work.lclpnet.combatctl.mixin.ScreenHandlerAccessor;
import work.lclpnet.kibu.hook.player.PlayerInventoryHooks;

import java.util.HashSet;
import java.util.Set;

public class CreativeInventoryHandler {

    private final Set<Entry> entries = new HashSet<>();

    public void init() {
        // When putting items from the creative inventory into the player inventory, a de-sync can happen with dynamic items.
        // Therefore, sync the slot at the end of the server tick

        PlayerInventoryHooks.MODIFIED_CREATIVE_INVENTORY.register(event
                -> entries.add(new Entry(event.player().networkHandler, event.slot())));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (entries.isEmpty()) return;

            for (var entry : entries) {
                ServerPlayerEntity player = entry.handler.player;

                if (player == null) continue;

                ScreenHandler screenHandler = player.playerScreenHandler;

                if (screenHandler == null) continue;

                ScreenHandlerSyncHandler syncHandler = ((ScreenHandlerAccessor) screenHandler).getSyncHandler();
                Slot slot = screenHandler.getSlot(entry.slot);

                if (slot == null || slot.inventory != player.getInventory()) continue;

                ItemStack stack = player.getInventory().getStack(slot.getIndex());

                syncHandler.updateSlot(screenHandler, entry.slot, stack);
            }

            entries.clear();
        });
    }

    private record Entry(ServerPlayNetworkHandler handler, int slot) {}
}
