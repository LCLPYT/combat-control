package work.lclpnet.combatctl.mixin;

import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(Item.class)
public interface ItemAccessor {

    @Accessor("ATTACK_DAMAGE_MODIFIER_ID")
    static UUID combatControl$getAttackDamageModifierUUID() {
        throw new RuntimeException();
    }
}
