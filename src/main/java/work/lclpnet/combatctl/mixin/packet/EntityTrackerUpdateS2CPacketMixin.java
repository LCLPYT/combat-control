package work.lclpnet.combatctl.mixin.packet;

import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import work.lclpnet.combatctl.type.ModifiablePacket;

@Mixin(EntityTrackerUpdateS2CPacket.class)
public class EntityTrackerUpdateS2CPacketMixin implements ModifiablePacket {

    @Unique private boolean modified = false;

    @Override
    public void combatControl$setModified() {
        modified = true;
    }

    @Override
    public boolean combatControl$isModified() {
        return modified;
    }
}
