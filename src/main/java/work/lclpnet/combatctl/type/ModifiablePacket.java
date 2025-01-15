package work.lclpnet.combatctl.type;

public interface ModifiablePacket {

    void combatControl$setModified();

    boolean combatControl$isModified();
}
