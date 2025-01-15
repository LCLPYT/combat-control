package work.lclpnet.combatctl.mixin.packet;

import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import work.lclpnet.combatctl.type.ModifiablePacket;

@Mixin(EntityEquipmentUpdateS2CPacket.class)
public class EntityEquipmentUpdateS2CPacketMixin implements ModifiablePacket {

    @Unique
    private boolean modified = false;

    @Override
    public void combatControl$setModified() {
        modified = true;
    }

    @Override
    public boolean combatControl$isModified() {
        return modified;
    }
}
