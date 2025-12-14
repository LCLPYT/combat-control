package work.lclpnet.combatctl.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import work.lclpnet.combatctl.impl.PingHandler;
import work.lclpnet.combatctl.type.CCServerPlayNetworkHandler;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin implements CCServerPlayNetworkHandler {

    @Shadow public ServerPlayer player;

    @Unique private final Object ccLock = new Object[0];
    @Unique private volatile PingHandler.Data pingData = null;

    @Override
    public PingHandler.@NotNull Data combatControl$getPingData() {
        if (pingData != null) {
            return pingData;
        }

        synchronized (ccLock) {
            if (pingData == null) {
                pingData = new PingHandler.Data();
            }
        }

        return pingData;
    }
}
