package work.lclpnet.combatctl.mixin.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface ItemStackRenderState$LayerRenderStateAccessor {

    @Accessor("itemTransform")
    ItemTransform combatControl$getItemTransform();
}
