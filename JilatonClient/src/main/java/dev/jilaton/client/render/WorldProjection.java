package dev.jilaton.client.render;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/** Projects camera-relative coordinates using a snapshot of vanilla's world matrices. */
public final class WorldProjection {
    private final Matrix4f matrix = new Matrix4f();
    private final Vector4f scratch = new Vector4f();
    private boolean valid;

    public void update(Matrix4fc projection, Matrix4fc view) { matrix.set(projection).mul(view); valid = true; }
    public void clear() { valid = false; }

    public boolean project(double x, double y, double z, int width, int height) {
        if (!valid || width <= 0 || height <= 0) return false;
        scratch.set((float) x, (float) y, (float) z, 1).mul(matrix);
        if (!Float.isFinite(scratch.w) || scratch.w <= 0.0001F) return false;
        scratch.div(scratch.w);
        return Float.isFinite(scratch.x) && Float.isFinite(scratch.y) && Float.isFinite(scratch.z)
                && scratch.z >= -1 && scratch.z <= 1 && Math.abs(scratch.x) <= 1.1 && Math.abs(scratch.y) <= 1.1;
    }

    public float x(int width) { return (scratch.x + 1) * width * 0.5F; }
    public float y(int height) { return (1 - scratch.y) * height * 0.5F; }
}
