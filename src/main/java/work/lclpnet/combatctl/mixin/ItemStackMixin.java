package work.lclpnet.combatctl.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.component.MergedComponentMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.impl.AttackAttributeHandler;
import work.lclpnet.combatctl.impl.SwordBlockingHandler;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow public abstract Item getItem();

    @Inject(
            method = "<init>(Lnet/minecraft/item/ItemConvertible;ILnet/minecraft/component/MergedComponentMap;)V",
            at = @At("TAIL")
    )
    public void combatControl$applyAttributeModifiers(ItemConvertible item, int count, MergedComponentMap components, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;

        AttackAttributeHandler._modifyAttackDamageAttribute(self);
    }

    @WrapOperation(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/Item;use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"
            )
    )
    private ActionResult combatControl$use(Item instance, World world, PlayerEntity user, Hand hand, Operation<ActionResult> original) {
        ItemStack stack = user.getActiveItem();

        if (stack != null && stack.getItem() instanceof SwordItem && hand != user.getActiveHand()) {
            return ActionResult.FAIL;
        }

        if (!(instance instanceof SwordItem) || !(user instanceof ServerPlayerEntity player)) {
            return original.call(instance, world, user, hand);
        }

        var control = CombatControl.get(player.getServer());

        if (!control.playerConfig(player).isSwordBlocking()) {
            return original.call(instance, world, user, hand);
        }

        // set using sword
        user.setCurrentHand(hand);

        // setup fake shield for vanilla players
        sendToNearbyVanillaPlayers(player, control, SwordBlockingHandler.fakeShieldEquipPacket(player));

        return ActionResult.CONSUME;
    }

    @WrapOperation(
            method = "onStoppedUsing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/Item;onStoppedUsing(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)Z"
            )
    )
    private boolean combatControl$onStoppedUsing(Item instance, ItemStack stack, World world, LivingEntity user, int remainingUseTicks, Operation<Boolean> original) {
        if (!(instance instanceof SwordItem) || !(user instanceof ServerPlayerEntity player)) {
            return original.call(instance, stack, world, user, remainingUseTicks);
        }

        var control = CombatControl.get(player.getServer());

        if (!control.playerConfig(player).isSwordBlocking()) {
            return original.call(instance, stack, world, user, remainingUseTicks);
        }

        // remove fake shield for vanilla players
        sendToNearbyVanillaPlayers(player, control, SwordBlockingHandler.fakeShieldUnequipPacket(player));

        return true;
    }

    @WrapMethod(method = "getMaxUseTime")
    private int combatControl$getMaxUseTime(LivingEntity user, Operation<Integer> original) {
        if (!(getItem() instanceof SwordItem) || !(user instanceof ServerPlayerEntity player)) {
            return original.call(user);
        }

        var control = CombatControl.get(player.getServer());

        if (!control.playerConfig(player).isSwordBlocking()) {
            return original.call(user);
        }

        return 72000;
    }

    @Unique
    private void sendToNearbyVanillaPlayers(ServerPlayerEntity player, CombatControl control, EntityEquipmentUpdateS2CPacket packet) {
        // PlayerLookup.tracking(player) does not guarantee that the player itself is part of it
        if (!control.hasModInstalled(player)) {
            player.networkHandler.sendPacket(packet);
        }

        for (ServerPlayerEntity other : PlayerLookup.tracking(player)) {
            if (control.hasModInstalled(other) || other == player) continue;

            other.networkHandler.sendPacket(packet);
        }
    }
}
