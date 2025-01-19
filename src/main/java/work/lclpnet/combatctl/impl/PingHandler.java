package work.lclpnet.combatctl.impl;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import work.lclpnet.combatctl.type.CCServerPlayNetworkHandler;
import work.lclpnet.combatctl.type.CombatControlServer;
import work.lclpnet.kibu.scheduler.KibuScheduling;
import work.lclpnet.kibu.scheduler.util.ChildScheduler;

import java.util.LinkedList;

import static java.lang.System.nanoTime;

public class PingHandler {

    private static final int
            TICK_RATE = 5,
            MAX_PING_REQUESTS = 20 / TICK_RATE * 10;

    private static final long CC_PING_ID = 0xCC_738129L | (1L << 63);  // some random constant value to identify combat control packets

    private final MinecraftServer server;
    private volatile ChildScheduler scheduler = null;

    public PingHandler(MinecraftServer server) {
        this.server = server;
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
            requestPing(player);
        }
    }

    private void requestPing(ServerPlayerEntity player) {
        if (!data(player).offerPingRequest(nanoTime())) return;

        // KeepAliveS2CPacket is handled immediately by the client in contrast to CommonPingS2CPacket
        player.networkHandler.sendPacket(new KeepAliveS2CPacket(CC_PING_ID));
    }

    public boolean receivePing(ServerPlayNetworkHandler handler, KeepAliveC2SPacket packet) {
        if (packet.getId() != CC_PING_ID) return false;

        Data data = data(handler);
        Long timestampNs = data.pollPingRequest();

        if (timestampNs == null) return false;

        data.prevPingMs = data.pingMs;
        data.pingMs = (nanoTime() - timestampNs) / 1000000.d;

        return true;
    }

    public @NotNull Data data(ServerPlayerEntity player) {
        return data(player.networkHandler);
    }

    public @NotNull Data data(ServerPlayNetworkHandler handler) {
        return ((CCServerPlayNetworkHandler) handler).combatControl$getPingData();
    }

    public static @NotNull PingHandler get(MinecraftServer server) {
        return ((CombatControlServer) server).combatControl$getPingHandler();
    }

    public static class Data {
        private final LinkedList<Long> pingRequests = new LinkedList<>();
        public double pingMs = 0, prevPingMs = 0;

        private synchronized @Nullable Long pollPingRequest() {
            return pingRequests.poll();
        }

        private synchronized boolean offerPingRequest(long timestamp) {
            if (pingRequests.size() >= MAX_PING_REQUESTS) {
                return false;
            }

            pingRequests.offer(timestamp);
            return true;
        }
    }
}
