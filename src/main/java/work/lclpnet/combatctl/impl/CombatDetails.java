package work.lclpnet.combatctl.impl;

import work.lclpnet.combatctl.api.CombatDetail;
import work.lclpnet.combatctl.impl.enums.Status;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CombatDetails {

    private static final Set<CombatDetail<?>> DETAILS = new HashSet<>();
    public static final CombatDetail<Status> ATTACK_COOLDOWN = register(CombatDetail.of(Status.ENABLED, Status.DISABLED));

    private static <T extends Enum<T>> CombatDetail<T> register(CombatDetail<T> detail) {
        DETAILS.add(detail);
        return detail;
    }

    public static Set<CombatDetail<?>> getDetails() {
        return Collections.unmodifiableSet(DETAILS);
    }

    private CombatDetails() {}
}
