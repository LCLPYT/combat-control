package work.lclpnet.combatctl.impl;

import it.unimi.dsi.fastutil.ints.IntDoublePair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.api.KnockbackVariant;
import work.lclpnet.combatctl.config.PlayerConfig;
import work.lclpnet.combatctl.mixin.CombatTrackerAccessor;
import work.lclpnet.combatctl.type.CombatControlServer;

import java.util.Optional;
import java.util.OptionalInt;

import static java.lang.Math.min;
import static java.lang.Math.round;
import static net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY;
import static work.lclpnet.combatctl.impl.PingHandler.pingOf;

public class KnockbackHandler {

    public static final double
            AIR_DRAG = 0.98,
            PING_TICK_COEFFICIENT = 0.02;

    private static final int MAX_SOLVER_TICKS = 35;

    public boolean applyKnockback(ServerPlayer player, Vec3 velocity, Vec3 knockbackDir, double strength) {
        if (isMovementAffected(player)) return false;

        PlayerConfig config = CombatControl.get(player.level().getServer()).playerConfig(player);

        KnockbackVariant variant = config.getKnockbackVariant();

        if (variant != KnockbackVariant.DEFAULT && player.hurtDuration != player.hurtTime) {
            var recentDamage = ((CombatTrackerAccessor) player.getCombatTracker()).getEntries();

            // do not apply knockback when attacked in damage grace period
            // this occurs if an attack in the grace period is stronger than the initial attack that caused the grace period
            if (recentDamage.isEmpty() || !recentDamage.getLast().source().is(DamageTypeTags.BYPASSES_COOLDOWN)) {
                return true;
            }
        }

        switch (variant) {
            case NO_SCALING -> {
                setRisingKnockback(player, velocity, knockbackDir, strength);
                return true;
            }
            case PING_ADJUSTED -> {
                if (player.hasEffect(MobEffects.LEVITATION)) return false;

                // functionality heavily inspired by KnockbackSync
                double groundDist = serverGroundDist(player);

                if (groundDist <= 2.e-02) return false;

                var sim = new SimulationState();

                if (simulateIsOnGround(player, groundDist, sim)) {
                    setRisingKnockback(player, velocity, knockbackDir, strength);
                } else {
                    setSimulatedKnockback(player, velocity, knockbackDir, sim);
                }

                return true;
            }
            case null, default -> {
                return false;
            }
        }
    }

    private static void setSimulatedKnockback(ServerPlayer player, Vec3 velocity, Vec3 knockbackDir, SimulationState state) {
        state.vy = player.getDeltaMovement().y();

        double grav = player.getAttributeValue(GRAVITY);
        int forwardTicks = (int) round(pingOf(player) * 0.5 * PING_TICK_COEFFICIENT);

        for (int i = 0; i < forwardTicks; i++) {
            eulerStep(state, grav);
        }

        player.setDeltaMovement(velocity.x / 2.0 - knockbackDir.x,
                state.vy,
                velocity.z / 2.0 - knockbackDir.z);
    }

    private static void setRisingKnockback(ServerPlayer player, Vec3 velocity, Vec3 knockbackDir, double strength) {
        player.setDeltaMovement(velocity.x / 2.0 - knockbackDir.x,
                Math.min(0.4, velocity.y / 2.0 + strength),
                velocity.z / 2.0 - knockbackDir.z);
    }

    private static boolean isMovementAffected(ServerPlayer player) {
        if (player.isFallFlying() || player.isInLiquid()) return true;

        BlockState state = player.getInBlockState();

        return state.is(Blocks.COBWEB) || state.is(Blocks.SCAFFOLDING);
    }

    private double serverGroundDist(ServerPlayer player) {
        // ray-cast down from the player in order to determine the distance
        CollisionContext shapeCtx = CollisionContext.of(player);
        ServerLevel world = player.level();
        AABB box = player.getBoundingBox();
        double y = player.getY();

        double maxDist = 10.d;

        maxDist = min(maxDist, rayCastDown(shapeCtx, world, box.minX, y, box.minZ, maxDist));
        maxDist = min(maxDist, rayCastDown(shapeCtx, world, box.minX, y, box.maxZ, maxDist));
        maxDist = min(maxDist, rayCastDown(shapeCtx, world, box.maxX, y, box.minZ, maxDist));
        maxDist = min(maxDist, rayCastDown(shapeCtx, world, box.maxX, y, box.maxZ, maxDist));

        return maxDist;
    }

    private double rayCastDown(CollisionContext shapeCtx, BlockGetter world, double x, double y, double z, double maxDist) {
        Vec3 start = new Vec3(x, y, z), end = start.subtract(0, maxDist, 0);

        BlockHitResult res = BlockGetter.traverseBlocks(start, end, null, (_ctx, pos) -> {
            BlockState state = world.getBlockState(pos);
            VoxelShape shape = ClipContext.Block.COLLIDER.get(state, world, pos, shapeCtx);

            return world.clipWithInteractionOverride(start, end, pos, shape, state);
        }, _ctx -> BlockHitResult.miss(end, Direction.DOWN, BlockPos.containing(end)));

        if (res.getType() != HitResult.Type.BLOCK) {
            return maxDist;
        }

        return res.getLocation().distanceTo(start);
    }

    private boolean simulateIsOnGround(ServerPlayer player, double groundDist, SimulationState sim) {
        if (groundDist > 1.3d || (player.isNoGravity() && groundDist > 2.e-2)) return false;

        double vy = player.getDeltaMovement().y();
        double grav = player.getAttributeValue(GRAVITY);

        var inAirTicks = inAirTicks(groundDist, vy, grav, sim);

        // formula from KnockbackSync, slightly rearranged
        return inAirTicks.isPresent() && round(pingOf(player) * 0.5 * PING_TICK_COEFFICIENT) >= inAirTicks.getAsInt();
    }

    private static OptionalInt inAirTicks(double groundDist, double vy, double grav, SimulationState sim) {
        // inspired by KnockbackSync
        sim.y = 0;
        sim.vy = vy;

        var highestPoint = vy > 0
                ? findHighestPoint(grav, sim)
                : Optional.of(IntDoublePair.of(0, 0));

        if (highestPoint.isEmpty()) {
            return OptionalInt.empty();
        }

        sim.y = 0;
        sim.vy = -Math.abs(vy);

        var fallTicks = fallTicks(grav, highestPoint.get().rightDouble() + groundDist, sim);

        if (fallTicks.isEmpty()) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(highestPoint.get().leftInt() + fallTicks.getAsInt());
    }

    private static Optional<IntDoublePair> findHighestPoint(final double gravity, SimulationState sim) {
        for (int tick = 0; tick < MAX_SOLVER_TICKS; tick++) {
            if (sim.vy <= 0) {
                //noinspection SuspiciousNameCombination
                return Optional.of(IntDoublePair.of(tick, sim.y));
            }

            eulerStep(sim, gravity);
        }

        return Optional.empty();
    }

    private static OptionalInt fallTicks(final double gravity, double fallDist, SimulationState sim) {
        for (int tick = 0; tick < MAX_SOLVER_TICKS; tick++) {
            if (-sim.y >= fallDist) {
                return OptionalInt.of(tick - 1);
            }

            eulerStep(sim, gravity);
        }

        return OptionalInt.empty();
    }

    private static void eulerStep(SimulationState sim, double gravity) {
        // apply gravity gradient
        sim.y += sim.vy;
        sim.vy = AIR_DRAG * (sim.vy - gravity);
    }

    public static @NotNull KnockbackHandler get(MinecraftServer server) {
        return ((CombatControlServer) server).combatControl$getKnockbackHandler();
    }

    private static class SimulationState {
        double y, vy;
    }
}
