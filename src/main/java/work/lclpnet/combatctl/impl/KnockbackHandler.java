package work.lclpnet.combatctl.impl;

import it.unimi.dsi.fastutil.ints.IntDoublePair;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.combatctl.api.CombatControl;
import work.lclpnet.combatctl.api.KnockbackVariant;
import work.lclpnet.combatctl.config.PlayerConfig;
import work.lclpnet.combatctl.mixin.DamageTrackerAccessor;
import work.lclpnet.combatctl.type.CombatControlServer;

import java.util.Optional;
import java.util.OptionalInt;

import static java.lang.Math.min;
import static java.lang.Math.round;
import static net.minecraft.entity.attribute.EntityAttributes.GRAVITY;
import static work.lclpnet.combatctl.impl.PingHandler.pingOf;

public class KnockbackHandler {

    public static final double
            AIR_DRAG = 0.98,
            PING_TICK_COEFFICIENT = 0.02;

    private static final int MAX_SOLVER_TICKS = 35;

    public boolean applyKnockback(ServerPlayerEntity player, Vec3d velocity, Vec3d knockbackDir, double strength) {
        if (isMovementAffected(player)) return false;

        PlayerConfig config = CombatControl.get(player.getServer()).playerConfig(player);

        KnockbackVariant variant = config.getKnockbackVariant();

        if (variant != KnockbackVariant.DEFAULT && player.maxHurtTime != player.hurtTime) {
            var recentDamage = ((DamageTrackerAccessor) player.getDamageTracker()).getRecentDamage();

            // do not apply knockback when attacked in damage grace period
            // this occurs if an attack in the grace period is stronger than the initial attack that caused the grace period
            if (recentDamage.isEmpty() || !recentDamage.getLast().damageSource().isIn(DamageTypeTags.BYPASSES_COOLDOWN)) {
                return true;
            }
        }

        switch (variant) {
            case NO_SCALING -> {
                setRisingKnockback(player, velocity, knockbackDir, strength);
                return true;
            }
            case PING_ADJUSTED -> {
                if (player.hasStatusEffect(StatusEffects.LEVITATION)) return false;

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

    private static void setSimulatedKnockback(ServerPlayerEntity player, Vec3d velocity, Vec3d knockbackDir, SimulationState state) {
        state.vy = player.getVelocity().getY();

        double grav = player.getAttributeValue(GRAVITY);
        int forwardTicks = (int) round(pingOf(player) * 0.5 * PING_TICK_COEFFICIENT);

        for (int i = 0; i < forwardTicks; i++) {
            eulerStep(state, grav);
        }

        player.setVelocity(velocity.x / 2.0 - knockbackDir.x,
                state.vy,
                velocity.z / 2.0 - knockbackDir.z);
    }

    private static void setRisingKnockback(ServerPlayerEntity player, Vec3d velocity, Vec3d knockbackDir, double strength) {
        player.setVelocity(velocity.x / 2.0 - knockbackDir.x,
                Math.min(0.4, velocity.y / 2.0 + strength),
                velocity.z / 2.0 - knockbackDir.z);
    }

    private static boolean isMovementAffected(ServerPlayerEntity player) {
        if (player.isGliding() || player.isInFluid()) return true;

        BlockState state = player.getBlockStateAtPos();

        return state.isOf(Blocks.COBWEB) || state.isOf(Blocks.SCAFFOLDING);
    }

    private double serverGroundDist(ServerPlayerEntity player) {
        // ray-cast down from the player in order to determine the distance
        ShapeContext shapeCtx = ShapeContext.of(player);
        ServerWorld world = player.getWorld();
        Box box = player.getBoundingBox();
        double y = player.getY();

        double maxDist = 10.d;

        maxDist = min(maxDist, rayCastDown(shapeCtx, world, box.minX, y, box.minZ, maxDist));
        maxDist = min(maxDist, rayCastDown(shapeCtx, world, box.minX, y, box.maxZ, maxDist));
        maxDist = min(maxDist, rayCastDown(shapeCtx, world, box.maxX, y, box.minZ, maxDist));
        maxDist = min(maxDist, rayCastDown(shapeCtx, world, box.maxX, y, box.maxZ, maxDist));

        return maxDist;
    }

    private double rayCastDown(ShapeContext shapeCtx, BlockView world, double x, double y, double z, double maxDist) {
        Vec3d start = new Vec3d(x, y, z), end = start.subtract(0, maxDist, 0);

        BlockHitResult res = BlockView.raycast(start, end, null, (_ctx, pos) -> {
            BlockState state = world.getBlockState(pos);
            VoxelShape shape = RaycastContext.ShapeType.COLLIDER.get(state, world, pos, shapeCtx);

            return world.raycastBlock(start, end, pos, shape, state);
        }, _ctx -> BlockHitResult.createMissed(end, Direction.DOWN, BlockPos.ofFloored(end)));

        if (res.getType() != HitResult.Type.BLOCK) {
            return maxDist;
        }

        return res.getPos().distanceTo(start);
    }

    private boolean simulateIsOnGround(ServerPlayerEntity player, double groundDist, SimulationState sim) {
        if (groundDist > 1.3d || (player.hasNoGravity() && groundDist > 2.e-2)) return false;

        double vy = player.getVelocity().getY();
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
