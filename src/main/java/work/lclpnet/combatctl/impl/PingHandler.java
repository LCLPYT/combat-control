package work.lclpnet.combatctl.impl;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.api.KnockbackVariant;
import work.lclpnet.combatctl.mixin.ServerCommonNetworkHandlerAccessor;
import work.lclpnet.combatctl.type.CCServerPlayNetworkHandler;
import work.lclpnet.combatctl.type.CombatControlServer;
import work.lclpnet.kibu.scheduler.KibuScheduling;
import work.lclpnet.kibu.scheduler.util.ChildScheduler;

import java.util.OptionalLong;

import static java.lang.Math.*;
import static java.lang.System.nanoTime;

public class PingHandler {

    private static final int TICK_RATE = 5;

    private static final long CC_PING_ID = 0xCC_738129L | (1L << 63);  // some random constant value to identify combat control packets

    private final MinecraftServer server;
    private final CombatControl combatControl;
    private volatile ChildScheduler scheduler = null;

    public PingHandler(MinecraftServer server) {
        this.server = server;
        this.combatControl = CombatControl.get(server);
    }

    public synchronized void init(Logger logger) {
        if (scheduler != null) return;

        scheduler = new ChildScheduler(KibuScheduling.getRootScheduler(), logger);
        scheduler.interval(this::tick, TICK_RATE);
    }

    public synchronized void destroy() {
        if (scheduler != null) {
            scheduler.detach();
        }
    }

    private void tick() {
        for (ServerPlayerEntity player : PlayerLookup.all(server)) {
            if (combatControl.playerConfig(player).getKnockbackVariant() != KnockbackVariant.PING_ADJUSTED) continue;

            requestPing(player);
        }
    }

    public void requestPing(ServerPlayerEntity player) {
        if (!data(player).offerPingRequest(nanoTime())) return;

        // KeepAliveS2CPacket is handled immediately by the client in contrast to CommonPingS2CPacket
        player.networkHandler.sendPacket(new KeepAliveS2CPacket(CC_PING_ID));
    }

    public boolean receivePing(ServerPlayNetworkHandler handler, KeepAliveC2SPacket packet) {
        if (packet.getId() != CC_PING_ID) return false;

        Data data = data(handler);
        var timestampNs = data.pollPingRequest();

        if (timestampNs.isEmpty()) return false;

        data.prevPingMs = data.pingMs;
        data.pingMs = (nanoTime() - timestampNs.getAsLong()) / 1000000.d;

        // smooth out lag spikes (when latency rises abruptly by a certain amount)
        boolean spike = data.pingMs - data.prevPingMs > max(1.d, combatControl.globalConfig().getPingSpikeMs());
        data.cleanedPingMs = spike ? (data.prevPingMs * 3.d + data.pingMs) * .25d : data.pingMs;

        ((ServerCommonNetworkHandlerAccessor) handler).setLatency((int) Math.round(data.cleanedPingMs));

        return true;
    }

    public static double pingOf(ServerPlayerEntity player) {
        return data(player).cleanedPingMs;
    }

    public static @NotNull Data data(ServerPlayerEntity player) {
        return data(player.networkHandler);
    }

    public static @NotNull Data data(ServerPlayNetworkHandler handler) {
        return ((CCServerPlayNetworkHandler) handler).combatControl$getPingData();
    }

    public static @NotNull PingHandler get(MinecraftServer server) {
        return ((CombatControlServer) server).combatControl$getPingHandler();
    }

    public static class Data {
        private static final int MAX_PING_REQUESTS = 20 / TICK_RATE * 10;

        private final LongRingBuf pingRequests = new LongRingBuf(MAX_PING_REQUESTS);
        public double pingMs = 0, prevPingMs = 0, cleanedPingMs = 0;

        private synchronized OptionalLong pollPingRequest() {
            if (pingRequests.count() == 0) {
                return OptionalLong.empty();
            }

            return OptionalLong.of(pingRequests.poll());
        }

        private synchronized boolean offerPingRequest(long timestamp) {
            if (pingRequests.count() >= MAX_PING_REQUESTS) {
                return false;
            }

            pingRequests.offer(timestamp);
            return true;
        }
    }

    private static class LongRingBuf {
        final long[] buf;
        int cursor = 0;
        int count = 0;

        LongRingBuf(int size) {
            buf = new long[size];
        }

        void offer(long l) {
            buf[cursor] = l;
            cursor = (cursor + 1) % buf.length;
            count = min(count + 1, buf.length);
        }

        long poll() {
            long l = buf[floorMod(cursor - count, buf.length)];
            count = max(0, count - 1);
            return l;
        }

        int count() {
            return count;
        }
    }
}
