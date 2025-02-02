package work.lclpnet.combatctl.impl;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.mixin.LivingEntityAccessor;
import work.lclpnet.combatctl.type.ModifiablePacket;
import work.lclpnet.kibu.hook.HookContainer;
import work.lclpnet.kibu.hook.network.ServerSendPacketCallback;
import work.lclpnet.kibu.hook.util.PendingResult;

import java.util.ArrayList;
import java.util.Arrays;
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

    private PendingResult<Packet<?>> onServerSendPacket(Packet<?> packet, ServerCommonNetworkHandler handler) {
        if (!(packet instanceof ModifiablePacket modifiablePacket)
                || modifiablePacket.combatControl$isModified()
                || !(handler instanceof ServerPlayNetworkHandler networkHandler)) {
            return PendingResult.pass();
        }

        ServerPlayerEntity player = networkHandler.player;
        var control = CombatControl.get(player.getServer());

        if (control.hasModInstalled(player)) {
            return PendingResult.pass();
        }

        ServerPlayerEntity related = relatedPlayer(player, packet);

        if (related == null) {
            return PendingResult.pass();
        }

        return switch (packet) {
            //noinspection DataFlowIssue
            case EntityTrackerUpdateS2CPacket orig -> modifyEntityTrackerPacket(orig);
            case EntityEquipmentUpdateS2CPacket orig -> modifyEquipmentPacket(orig, related);
            default -> PendingResult.pass();
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

    private PendingResult<Packet<?>> modifyEquipmentPacket(EntityEquipmentUpdateS2CPacket orig, ServerPlayerEntity related) {
        // make sure that the vanilla player holds a shield in their other hand, so that the blocking animation can be displayed
        EquipmentSlot slot = otherHandSlot(related);

        var entry = orig.getEquipmentList().stream()
                .filter(pair -> pair.getFirst() == slot)
                .findAny()
                .orElse(null);

        if (entry != null && entry.getSecond().isOf(Items.SHIELD)) {
            return PendingResult.pass();
        }

        var list = new ArrayList<>(orig.getEquipmentList());

        if (entry != null) {
            list.remove(entry);
        }

        list.add(Pair.of(slot, new ItemStack(Items.SHIELD)));

        return PendingResult.of(new EntityEquipmentUpdateS2CPacket(orig.getEntityId(), list));
    }

    private static @NotNull EquipmentSlot otherHandSlot(ServerPlayerEntity related) {
        return related.getActiveHand() == Hand.MAIN_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
    }

    private static PendingResult<Packet<?>> modifyEntityTrackerPacket(EntityTrackerUpdateS2CPacket orig) {
        var entries = orig.trackedValues();

        // search living flags entry to invert the offhand flag, as the player will hold a fake shield in their other hand
        for (int i = 0, size = entries.size(); i < size; i++) {
            var entry = entries.get(i);

            if (entry.id() != LIVING_FLAGS.id()) continue;

            byte flags = (byte) entry.value();
            flags ^= (byte) OFF_HAND_ACTIVE_FLAG;

            var modified = modifiedTrackerPacket(orig.id(), entries, i, DataTracker.SerializedEntry.of(LIVING_FLAGS, flags));

            return PendingResult.of(modified);
        }

        return PendingResult.pass();
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

        return new EntityTrackerUpdateS2CPacket(entityId, newEntries);
    }

    private static <T extends Packet<?>> T modified(T packet) {
        ((ModifiablePacket) packet).combatControl$setModified();
        return packet;
    }

    public static EntityEquipmentUpdateS2CPacket fakeShieldEquipPacket(ServerPlayerEntity related) {
        EquipmentSlot otherHand = otherHandSlot(related);

        var list = Arrays.stream(EquipmentSlot.values())
                .map(slot -> Pair.of(slot, slot == otherHand
                        ? new ItemStack(Items.SHIELD)
                        : related.getEquippedStack(slot)))
                .toList();

        var packet = new EntityEquipmentUpdateS2CPacket(related.getId(), list);

        return modified(packet);
    }

    public static EntityEquipmentUpdateS2CPacket fakeShieldUnequipPacket(ServerPlayerEntity related) {
        var list = Arrays.stream(EquipmentSlot.values())
                .map(slot -> Pair.of(slot, related.getEquippedStack(slot)))
                .toList();

        var packet = new EntityEquipmentUpdateS2CPacket(related.getId(), list);

        return modified(packet);
    }

    public static void sendToNearbyVanillaPlayers(ServerPlayerEntity player, CombatControl control, EntityEquipmentUpdateS2CPacket packet, boolean withSelf) {
        if (withSelf && !control.hasModInstalled(player)) {
            player.networkHandler.sendPacket(packet);
        }

        for (ServerPlayerEntity other : PlayerLookup.tracking(player)) {
            if (other == player || control.hasModInstalled(other)) continue;

            other.networkHandler.sendPacket(packet);
        }
    }

    public static ActionResult useItem(Item item, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getActiveItem();

        if (stack != null && stack.getItem() instanceof SwordItem && hand != user.getActiveHand()) {
            return ActionResult.FAIL;
        }

        if (!(item instanceof SwordItem) || !(user instanceof ServerPlayerEntity player) || shieldTakesPrecence(player, hand)) {
            return ActionResult.PASS;
        }

        var control = CombatControl.get(player.getServer());

        if (!control.playerConfig(player).isSwordBlocking()) {
            return ActionResult.PASS;
        }

        // set using sword
        user.setCurrentHand(hand);

        // setup fake shield for vanilla players
        sendToNearbyVanillaPlayers(player, control, SwordBlockingHandler.fakeShieldEquipPacket(player), true);

        return ActionResult.CONSUME;
    }

    public static boolean stopUsing(Item item, LivingEntity user) {
        if (!(item instanceof SwordItem) || !(user instanceof ServerPlayerEntity player)) {
            return false;
        }

        var control = CombatControl.get(player.getServer());

        if (!control.playerConfig(player).isSwordBlocking()) {
            return false;
        }

        // remove fake shield for vanilla players
        sendToNearbyVanillaPlayers(player, control, SwordBlockingHandler.fakeShieldUnequipPacket(player), true);

        return true;
    }

    public static boolean canBlockWith(LivingEntity user, Item item) {
        if (!(item instanceof SwordItem) || !(user instanceof ServerPlayerEntity player)) {
            return false;
        }

        return CombatControl.get(player.getServer()).playerConfig(player).isSwordBlocking();
    }

    public static boolean shieldTakesPrecence(PlayerEntity player, Hand hand) {
        return hand == Hand.MAIN_HAND && player.getOffHandStack().isOf(Items.SHIELD);
    }
}
