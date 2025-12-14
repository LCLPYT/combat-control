package work.lclpnet.combatctl.mixin;

import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.impl.PingHandler;

@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonPacketListenerImplMixin {

    @Shadow @Final protected MinecraftServer server;

    @Inject(
            method = "handleKeepAlive",
            at = @At("HEAD"),
            cancellable = true
    )
    private void combatControl$onKeepAlive(ServerboundKeepAlivePacket packet, CallbackInfo ci) {
        if ((Object) this instanceof ServerGamePacketListenerImpl handler) {
            if (PingHandler.get(server).receivePing(handler, packet)) {
                ci.cancel();
            }
        }
    }
}
