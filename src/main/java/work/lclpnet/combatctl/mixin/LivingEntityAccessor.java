package work.lclpnet.combatctl.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    @Accessor("LIVING_FLAGS")
    static TrackedData<Byte> getLivingFlagsTrackedData() {
        throw new AssertionError();
    }

    @Accessor("OFF_HAND_ACTIVE_FLAG")
    static int getOffHandActiveFlag() {
        throw new AssertionError();
    }
}
