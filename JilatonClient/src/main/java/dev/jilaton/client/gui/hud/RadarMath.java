package dev.jilaton.client.gui.hud;

/**
 * World offsets to radar pixels using Minecraft's yaw convention: 0 faces south (+Z),
 * 90 faces west (-X). Screen coordinates grow right and down from the radar centre.
 */
public final class RadarMath {
    public record Point(double x, double y, double distance, boolean insideRange) {
    }

    private RadarMath() {
    }

    public static Point project(double dx, double dz, float yawDegrees, boolean northUp,
                                double rangeBlocks, double radiusPixels) {
        if (!Double.isFinite(dx) || !Double.isFinite(dz) || !Float.isFinite(yawDegrees)) {
            throw new IllegalArgumentException("Radar offsets must be finite");
        }
        if (!(rangeBlocks > 0) || !(radiusPixels > 0)) {
            throw new IllegalArgumentException("Radar range and radius must be positive");
        }
        double distance = Math.hypot(dx, dz);
        double rightX, rightZ, forwardX, forwardZ;
        if (northUp) {
            rightX = 1;
            rightZ = 0;
            forwardX = 0;
            forwardZ = -1;
        } else {
            double yaw = Math.toRadians(yawDegrees);
            rightX = Math.cos(yaw);
            rightZ = Math.sin(yaw);
            forwardX = -Math.sin(yaw);
            forwardZ = Math.cos(yaw);
        }
        double x = dx * rightX + dz * rightZ;
        double y = -(dx * forwardX + dz * forwardZ);
        boolean insideRange = distance <= rangeBlocks;
        double scale = insideRange ? radiusPixels / rangeBlocks : radiusPixels / distance;
        return new Point(x * scale, y * scale, distance, insideRange);
    }
}
