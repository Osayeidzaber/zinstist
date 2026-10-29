package dev.jilaton.client.module.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Projectile intercept calculations using per-tick drag and gravity. */
public final class BowAimMath {
    private static final double TIME_STEP = 0.25;
    private static final double MAX_FLIGHT_TICKS = 100.0;
    private static final double ROOT_TOLERANCE = 1.0E-7;

    private BowAimMath() {
    }

    public static Optional<Solution> solve(double originX, double originY, double originZ,
                                           double targetX, double targetY, double targetZ,
                                           double targetVelocityX, double targetVelocityY, double targetVelocityZ,
                                           double projectileSpeed, double gravity, double drag,
                                           boolean predictMovement, boolean highArc) {
        if (!finite(originX, originY, originZ, targetX, targetY, targetZ, targetVelocityX, targetVelocityY,
                targetVelocityZ, projectileSpeed, gravity, drag)
                || projectileSpeed <= 0.0 || gravity < 0.0 || drag <= 0.0 || drag > 1.0) {
            return Optional.empty();
        }

        double velocityX = predictMovement ? targetVelocityX : 0.0;
        double velocityY = predictMovement ? targetVelocityY : 0.0;
        double velocityZ = predictMovement ? targetVelocityZ : 0.0;
        List<Double> roots = new ArrayList<>(2);
        double previousTime = TIME_STEP;
        double previousError = speedError(previousTime, originX, originY, originZ,
                targetX, targetY, targetZ, velocityX, velocityY, velocityZ,
                projectileSpeed, gravity, drag);
        for (double time = previousTime + TIME_STEP; time <= MAX_FLIGHT_TICKS; time += TIME_STEP) {
            double error = speedError(time, originX, originY, originZ,
                    targetX, targetY, targetZ, velocityX, velocityY, velocityZ,
                    projectileSpeed, gravity, drag);
            if (Double.isFinite(previousError) && Double.isFinite(error)
                    && Math.signum(previousError) != Math.signum(error)) {
                roots.add(refineRoot(previousTime, time, previousError, originX, originY, originZ,
                        targetX, targetY, targetZ, velocityX, velocityY, velocityZ,
                        projectileSpeed, gravity, drag));
            }
            previousTime = time;
            previousError = error;
        }

        double time;
        if (!roots.isEmpty()) {
            time = highArc ? roots.get(roots.size() - 1) : roots.get(0);
        } else {
            time = bestApproximateTime(originX, originY, originZ, targetX, targetY, targetZ,
                    velocityX, velocityY, velocityZ, projectileSpeed, gravity, drag);
            if (!Double.isFinite(time)) {
                return Optional.empty();
            }
        }

        Kinematics kinematics = kinematics(time, originX, originY, originZ,
                targetX, targetY, targetZ, velocityX, velocityY, velocityZ, gravity, drag);
        if (kinematics == null || Math.hypot(kinematics.dx(), kinematics.dz()) < 1.0E-7) {
            return Optional.empty();
        }
        double horizontalSpeed = Math.hypot(kinematics.dx(), kinematics.dz()) / kinematics.pathFactor();
        double verticalSpeed = (kinematics.dy() - kinematics.gravityDrop()) / kinematics.pathFactor();
        double requiredSpeed = Math.hypot(horizontalSpeed, verticalSpeed);
        double residual = Math.abs(requiredSpeed - projectileSpeed);
        if (!Double.isFinite(residual) || residual > Math.max(0.03, projectileSpeed * 0.02)) {
            return Optional.empty();
        }

        float yaw = (float) Math.toDegrees(Math.atan2(kinematics.dz(), kinematics.dx())) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(verticalSpeed, horizontalSpeed));
        return Optional.of(new Solution(yaw, pitch, time, residual));
    }

    private static double refineRoot(double low, double high, double lowError,
                                     double originX, double originY, double originZ,
                                     double targetX, double targetY, double targetZ,
                                     double velocityX, double velocityY, double velocityZ,
                                     double speed, double gravity, double drag) {
        for (int iteration = 0; iteration < 32 && high - low > ROOT_TOLERANCE; iteration++) {
            double middle = (low + high) * 0.5;
            double middleError = speedError(middle, originX, originY, originZ,
                    targetX, targetY, targetZ, velocityX, velocityY, velocityZ, speed, gravity, drag);
            if (Math.signum(lowError) == Math.signum(middleError)) {
                low = middle;
                lowError = middleError;
            } else {
                high = middle;
            }
        }
        return (low + high) * 0.5;
    }

    private static double bestApproximateTime(double originX, double originY, double originZ,
                                               double targetX, double targetY, double targetZ,
                                               double velocityX, double velocityY, double velocityZ,
                                               double speed, double gravity, double drag) {
        double bestTime = Double.NaN;
        double bestError = Double.POSITIVE_INFINITY;
        for (double time = TIME_STEP; time <= MAX_FLIGHT_TICKS; time += TIME_STEP) {
            double error = Math.abs(speedError(time, originX, originY, originZ,
                    targetX, targetY, targetZ, velocityX, velocityY, velocityZ, speed, gravity, drag));
            if (error < bestError) {
                bestError = error;
                bestTime = time;
            }
        }
        return bestError <= Math.max(0.03, speed * 0.02) ? bestTime : Double.NaN;
    }

    private static double speedError(double time, double originX, double originY, double originZ,
                                     double targetX, double targetY, double targetZ,
                                     double velocityX, double velocityY, double velocityZ,
                                     double speed, double gravity, double drag) {
        Kinematics kinematics = kinematics(time, originX, originY, originZ,
                targetX, targetY, targetZ, velocityX, velocityY, velocityZ, gravity, drag);
        if (kinematics == null) {
            return Double.NaN;
        }
        double horizontalSpeed = Math.hypot(kinematics.dx(), kinematics.dz()) / kinematics.pathFactor();
        double verticalSpeed = (kinematics.dy() - kinematics.gravityDrop()) / kinematics.pathFactor();
        return Math.hypot(horizontalSpeed, verticalSpeed) - speed;
    }

    private static Kinematics kinematics(double time, double originX, double originY, double originZ,
                                         double targetX, double targetY, double targetZ,
                                         double velocityX, double velocityY, double velocityZ,
                                         double gravity, double drag) {
        double factor = pathFactor(time, drag);
        if (!Double.isFinite(factor) || factor <= 0.0) {
            return null;
        }
        double futureX = targetX + velocityX * time;
        double futureY = targetY + velocityY * time;
        double futureZ = targetZ + velocityZ * time;
        double gravityDrop = gravityPath(time, drag, gravity);
        return new Kinematics(futureX - originX, futureY - originY, futureZ - originZ, factor, gravityDrop);
    }

    private static double pathFactor(double time, double drag) {
        if (Math.abs(1.0 - drag) < 1.0E-9) {
            return time;
        }
        return -Math.expm1(time * Math.log(drag)) / (1.0 - drag);
    }

    private static double gravityPath(double time, double drag, double gravity) {
        if (gravity == 0.0 || time <= 1.0) {
            return 0.0;
        }
        if (Math.abs(1.0 - drag) < 1.0E-9) {
            return -0.5 * gravity * time * (time - 1.0);
        }
        double factor = pathFactor(time, drag);
        return -(gravity / (1.0 - drag)) * (time - factor);
    }

    private static boolean finite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    private record Kinematics(double dx, double dy, double dz, double pathFactor, double gravityDrop) {
    }

    public record Solution(float yaw, float pitch, double flightTicks, double speedResidual) {
    }
}
