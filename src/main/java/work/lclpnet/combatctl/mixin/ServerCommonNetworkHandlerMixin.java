package work.lclpnet.combatctl.mixin;

import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.impl.PingHandler;

@Mixin(ServerCommonNetworkHandler.class)
public class ServerCommonNetworkHandlerMixin {

    @Shadow @Final protected MinecraftServer server;

    @Inject(
            method = "onKeepAlive",
            at = @At("HEAD"),
            cancellable = true
    )
    private void combatControl$onKeepAlive(KeepAliveC2SPacket packet, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayNetworkHandler handler) {
            if (PingHandler.get(server).receivePing(handler, packet)) {
                ci.cancel();
            }
        }
    }
}
