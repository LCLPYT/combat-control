package work.lclpnet.combatctl.impl;

import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.mixin.LivingEntityAccessor;
import work.lclpnet.combatctl.type.ModifiablePacket;
import work.lclpnet.kibu.hook.HookContainer;
import work.lclpnet.kibu.hook.network.ServerSendPacketCallback;

import java.util.ArrayList;
import java.util.List;

public class SwordBlockingHandler {

    private static final TrackedData<Byte> LIVING_FLAGS = LivingEntityAccessor.getLivingFlagsTrackedData();
    private static final int OFF_HAND_ACTIVE_FLAG = LivingEntityAccessor.getOffHandActiveFlag();
    private final HookContainer hooks = new HookContainer();

    public void init() {
        hooks.registerHook(ServerSendPacketCallback.HOOK, this::onServerSendPacket);
    }

    public void destroy() {
        hooks.unload();
    }

    private boolean onServerSendPacket(Packet<?> packet, ServerCommonNetworkHandler handler) {
        if (!(packet instanceof ModifiablePacket modifiablePacket)
                || modifiablePacket.combatControl$isModified()
                || !(handler instanceof ServerPlayNetworkHandler networkHandler)) {
            return false;
        }

        ServerPlayerEntity player = networkHandler.player;
        var control = CombatControl.get(player.getServer());

        if (control.hasModInstalled(player)) {
            return false;
        }

        ServerPlayerEntity related = relatedPlayer(player, packet);

        if (related == null) {
            return false;
        }

        return switch (packet) {
            //noinspection DataFlowIssue
            case EntityTrackerUpdateS2CPacket orig -> modifyEntityTrackerPacket(handler, orig);
            case EntityEquipmentUpdateS2CPacket orig -> modifyEquipmentPacket(handler, orig, related);
            default -> false;
        };
    }

    private @Nullable ServerPlayerEntity relatedPlayer(ServerPlayerEntity player, Packet<?> packet) {
        int entityId = switch (packet) {
            case EntityTrackerUpdateS2CPacket orig -> orig.id();
            case EntityEquipmentUpdateS2CPacket orig -> orig.getEntityId();
            default -> -1;
        };

        if (entityId == -1 || !(player.getServerWorld().getEntityById(entityId) instanceof ServerPlayerEntity related)) {
            return null;
        }

        var control = CombatControl.get(player.getServer());

        if (!control.playerConfig(related).isSwordBlocking() || !related.isUsingItem()) {
            return null;
        }

        ItemStack stack = related.getActiveItem();

        if (stack == null || !(stack.getItem() instanceof SwordItem)) {
            return null;
        }

        return related;
    }

    private boolean modifyEquipmentPacket(ServerCommonNetworkHandler handler, EntityEquipmentUpdateS2CPacket orig, ServerPlayerEntity related) {
        // make sure that the vanilla player holds a shield in their other hand, so that the blocking animation can be displayed
        EquipmentSlot slot = otherHandSlot(related);

        var entry = orig.getEquipmentList().stream()
                .filter(pair -> pair.getFirst() == slot)
                .findAny()
                .orElse(null);

        if (entry != null && entry.getSecond().isOf(Items.SHIELD)) {
            return false;
        }

        var list = new ArrayList<>(orig.getEquipmentList());

        if (entry != null) {
            list.remove(entry);
        }

        list.add(Pair.of(slot, new ItemStack(Items.SHIELD)));

        handler.sendPacket(modified(new EntityEquipmentUpdateS2CPacket(orig.getEntityId(), list)));

        return true;
    }

    private static @NotNull EquipmentSlot otherHandSlot(ServerPlayerEntity related) {
        return related.getActiveHand() == Hand.MAIN_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
    }

    private static boolean modifyEntityTrackerPacket(ServerCommonNetworkHandler handler, EntityTrackerUpdateS2CPacket orig) {
        var entries = orig.trackedValues();

        // search living flags entry to invert the offhand flag, as the player will hold a fake shield in their other hand
        for (int i = 0, size = entries.size(); i < size; i++) {
            var entry = entries.get(i);

            if (entry.id() != LIVING_FLAGS.id()) continue;

            byte flags = (byte) entry.value();
            flags ^= (byte) OFF_HAND_ACTIVE_FLAG;

            handler.sendPacket(modifiedTrackerPacket(orig.id(), entries, i, DataTracker.SerializedEntry.of(LIVING_FLAGS, flags)));

            return true;
        }

        return false;
    }

    private static EntityTrackerUpdateS2CPacket modifiedTrackerPacket(int entityId, List<DataTracker.SerializedEntry<?>> entries, int idx, DataTracker.SerializedEntry<Byte> newEntry) {
        final int size = entries.size();
        var newEntries = new ArrayList<DataTracker.SerializedEntry<?>>(size);

        // copy entries before
        for (int j = 0; j < idx; j++) {
            newEntries.add(entries.get(idx));
        }

        newEntries.add(newEntry);

        // copy entries after
        for (int j = idx + 1; j < size; j++) {
            newEntries.add(entries.get(idx));
        }

        return modified(new EntityTrackerUpdateS2CPacket(entityId, newEntries));
    }

    private static <T extends Packet<?>> T modified(T packet) {
        ((ModifiablePacket) packet).combatControl$setModified();
        return packet;
    }

    public static EntityEquipmentUpdateS2CPacket fakeShieldEquipPacket(ServerPlayerEntity related) {
        var list = List.of(Pair.of(otherHandSlot(related), new ItemStack(Items.SHIELD)));
        var packet = new EntityEquipmentUpdateS2CPacket(related.getId(), list);

        return modified(packet);
    }

    public static EntityEquipmentUpdateS2CPacket fakeShieldUnequipPacket(ServerPlayerEntity related) {
        EquipmentSlot slot = otherHandSlot(related);
        ItemStack stack = related.getEquippedStack(slot);

        var packet = new EntityEquipmentUpdateS2CPacket(related.getId(), List.of(Pair.of(slot, stack)));

        return modified(packet);
    }
}
