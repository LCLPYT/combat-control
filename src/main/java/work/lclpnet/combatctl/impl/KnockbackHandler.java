package work.lclpnet.combatctl.impl;

import it.unimi.dsi.fastutil.ints.IntDoublePair;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
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
import work.lclpnet.combatctl.type.CombatControlServer;

import java.util.Optional;
import java.util.OptionalInt;

import static java.lang.Math.min;
import static java.lang.Math.round;
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

        switch (variant) {
            case NO_SCALING -> {
                player.setVelocity(velocity.x / 2.0 - knockbackDir.x,
                        Math.min(0.4, velocity.y / 2.0 + strength),
                        velocity.z / 2.0 - knockbackDir.z);
                return true;
            }
            case PING_ADJUSTED -> {
                if (player.hasStatusEffect(StatusEffects.LEVITATION)) return false;

                // heavily inspired by KnockbackSync
                double groundDist = serverGroundDist(player);
                System.out.println(groundDist);

                if (groundDist <= 2.e-02) {
                    System.out.println("on ground server");
                    return false;
                }

                if (isOnGroundWRTPing(player, groundDist)) {
                    System.out.println("on ground client");
                    // TODO implement
                } else {
                    System.out.println("in air client");
                    // TODO implement
                }

                return true;
            }
            case null, default -> {
                return false;
            }
        }
    }

    private static boolean isMovementAffected(ServerPlayerEntity player) {
        if (player.isGliding() || player.isInFluid()) return true;

        BlockState state = player.getBlockStateAtPos();

        return state.isOf(Blocks.COBWEB) || state.isOf(Blocks.SCAFFOLDING);
    }

    private double serverGroundDist(ServerPlayerEntity player) {
        // ray-cast down from the player in order to determine the distance
        ShapeContext shapeCtx = ShapeContext.of(player);
        ServerWorld world = player.getServerWorld();
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

    private boolean isOnGroundWRTPing(ServerPlayerEntity player, double groundDist) {
        if (groundDist > 1.3d || (player.hasNoGravity() && groundDist > 2.e-2)) return false;

        double vy = player.getVelocity().getY();
        double grav = player.getAttributeValue(EntityAttributes.GRAVITY);

        var inAirTicks = inAirTicks(groundDist, vy, grav);

        return inAirTicks.isPresent() && round(pingOf(player) * PING_TICK_COEFFICIENT) >= inAirTicks.getAsInt();
    }

    private static OptionalInt inAirTicks(double groundDist, double vy, double grav) {
        var state = new State(0, vy);

        var highestPoint = vy > 0
                ? findHighestPoint(grav, state)
                : Optional.of(IntDoublePair.of(0, 0));

        if (highestPoint.isEmpty()) {
            return OptionalInt.empty();
        }

        state.y = 0;
        state.vy = -Math.abs(vy);

        var fallTicks = fallTicks(grav, highestPoint.get().rightDouble() + groundDist, state);

        if (fallTicks.isEmpty()) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(highestPoint.get().leftInt() + fallTicks.getAsInt());
    }

    private static Optional<IntDoublePair> findHighestPoint(final double gravity, State state) {
        for (int tick = 0; tick < MAX_SOLVER_TICKS; tick++) {
            if (state.vy <= 0) {
                //noinspection SuspiciousNameCombination
                return Optional.of(IntDoublePair.of(tick, state.y));
            }

            eulerStep(state, gravity);
        }

        return Optional.empty();
    }

    private static OptionalInt fallTicks(final double gravity, double fallDist, State state) {
        for (int tick = 0; tick < MAX_SOLVER_TICKS; tick++) {
            if (-state.y >= fallDist) {
                return OptionalInt.of(tick - 1);
            }

            eulerStep(state, gravity);
        }

        return OptionalInt.empty();
    }

    private static void eulerStep(State state, double gravity) {
        state.y += state.vy;
        state.vy = AIR_DRAG * (state.vy - gravity);
    }

    public static @NotNull KnockbackHandler get(MinecraftServer server) {
        return ((CombatControlServer) server).combatControl$getKnockbackHandler();
    }

    private static class State {
        double y, vy;

        State(double y, double vy) {
            this.y = y;
            this.vy = vy;
        }
    }
}
