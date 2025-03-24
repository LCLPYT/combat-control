package work.lclpnet.combatctl.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import work.lclpnet.combatctl.api.CombatControlClient;
import work.lclpnet.combatctl.type.ToolInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @WrapOperation(
            method = "getArmPose(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/Hand;)Lnet/minecraft/client/render/entity/model/BipedEntityModel$ArmPose;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/item/consume/UseAction;"
            )
    )
    private static UseAction combatControl$getUseAction(ItemStack instance, Operation<UseAction> original) {
        if (CombatControlClient.get().abilities().swordBlocking && ToolInfo.of(instance).filter(ToolInfo::isSword).isPresent()) {
            return UseAction.BLOCK;
        }

        return original.call(instance);
    }
}
