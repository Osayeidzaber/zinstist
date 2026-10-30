package dev.jilaton.client.render;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ParticleBurstMath {
    private ParticleBurstMath() { }

    public static Vec3 nearestPoint(AABB box, Vec3 eye) {
        return new Vec3(Math.max(box.minX, Math.min(box.maxX, eye.x)),
                Math.max(box.minY, Math.min(box.maxY, eye.y)), Math.max(box.minZ, Math.min(box.maxZ, eye.z)));
    }

    public static Vec3 pointInside(AABB box, double x, double y, double z) {
        return new Vec3(box.minX + box.getXsize() * (0.1 + 0.8 * x),
                box.minY + box.getYsize() * (0.1 + 0.8 * y), box.minZ + box.getZsize() * (0.1 + 0.8 * z));
    }

    public static boolean accepts(String filter, boolean living, boolean player) {
        return filter.equals("All") || (filter.equals("Players") ? player : living);
    }
}
