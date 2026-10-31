package dev.jilaton.client.render.model;

import dev.jilaton.client.render.fx.FxFrame;

/**
 * Submit path for {@link Mesh} geometry. Everything is transformed into camera-relative space here, then
 * queued into the frame's {@code EffectBatch}, which draws through the shared world pipeline: depth-test
 * against terrain, translucent blending, no depth writes.
 *
 * <p>Because that pipeline never writes depth, two things have to happen on the CPU instead: back faces
 * are removed with a winding test (the pipeline itself is double sided), and placed meshes are painted
 * back to front so overlapping translucent surfaces blend in the right order. Every vertex is also nudged
 * a fraction toward the camera, which keeps co-planar surfaces (a feather's two faces, a halo over the
 * hair) out of a depth tie.
 */
public final class ModelBatch {
    /** 16 players at ULTRA Feather wings is 16 x 60 = 960 parts, so 512 silently truncated a full server. */
    private static final int MAX_PLACED = 1024;
    private static final double DEPTH_BIAS = 0.0015;
    private static final double LIGHT_X = 0.379;
    private static final double LIGHT_Y = 0.867;
    private static final double LIGHT_Z = 0.323;

    private final Mesh[] meshes = new Mesh[MAX_PLACED];
    private final Material[] materials = new Material[MAX_PLACED];
    private final Transform[] transforms = new Transform[MAX_PLACED];
    private final double[] scales = new double[MAX_PLACED];
    private final double[] opacity = new double[MAX_PLACED];
    private final double[] depth = new double[MAX_PLACED];
    private final boolean[] walls = new boolean[MAX_PLACED];
    private final int[] order = new int[MAX_PLACED];
    private final double[] normal = new double[3];
    private final double[] scratch = new double[3];
    private int count;

    public void reset() { count = 0; }

    public int size() { return count; }

    public void add(FxFrame frame, Mesh mesh, Material material, Transform transform,
                    double scale, double opacity, boolean throughWalls) {
        if (count >= MAX_PLACED || opacity <= 0.004) return;
        meshes[count] = mesh;
        materials[count] = material;
        transforms[count] = transform;
        scales[count] = scale;
        this.opacity[count] = opacity;
        walls[count] = throughWalls;
        transform.point(mesh.centreX() * scale, mesh.centreY() * scale, mesh.centreZ() * scale, scratch);
        double dx = scratch[0] - frame.camX, dy = scratch[1] - frame.camY, dz = scratch[2] - frame.camZ;
        depth[count] = dx * dx + dy * dy + dz * dz;
        order[count] = count;
        count++;
    }

    /** Back to front, then one software-culled quad at a time into the frame batch. */
    public void flush(FxFrame frame) {
        sortDescending();
        double alphaScale = Math.max(0, Math.min(1, frame.intensity));
        for (int slot = 0; slot < count; slot++) {
            int index = order[slot];
            Mesh mesh = meshes[index];
            Material material = materials[index];
            Transform transform = transforms[index];
            double scale = scales[index];
            double alpha = this.opacity[index] * alphaScale;
            if (alpha <= 0.004) continue;
            boolean throughWalls = walls[index];
            float[] positions = mesh.positions();
            float[] normals = mesh.normals();
            float[] tones = mesh.tones();
            int[] indices = mesh.indices();
            for (int quad = 0; quad < indices.length; quad += 4) {
                int v0 = indices[quad], v1 = indices[quad + 1], v2 = indices[quad + 2], v3 = indices[quad + 3];
                double ax = transformX(transform, positions, v0, scale);
                double ay = transformY(transform, positions, v0, scale);
                double az = transformZ(transform, positions, v0, scale);
                double bx = transformX(transform, positions, v1, scale);
                double by = transformY(transform, positions, v1, scale);
                double bz = transformZ(transform, positions, v1, scale);
                double cx = transformX(transform, positions, v2, scale);
                double cy = transformY(transform, positions, v2, scale);
                double cz = transformZ(transform, positions, v2, scale);
                double dx = transformX(transform, positions, v3, scale);
                double dy = transformY(transform, positions, v3, scale);
                double dz = transformZ(transform, positions, v3, scale);
                double mx = (ax + bx + cx + dx) * 0.25 - frame.camX;
                double my = (ay + by + cy + dy) * 0.25 - frame.camY;
                double mz = (az + bz + cz + dz) * 0.25 - frame.camZ;
                if (!facesCamera(ax - frame.camX, ay - frame.camY, az - frame.camZ,
                        bx - frame.camX, by - frame.camY, bz - frame.camZ,
                        cx - frame.camX, cy - frame.camY, cz - frame.camZ, mx, my, mz)) continue;
                double biasX = 0, biasY = 0, biasZ = 0;
                double ml = Math.sqrt(mx * mx + my * my + mz * mz);
                if (ml > 1e-5) {
                    biasX = mx / ml * DEPTH_BIAS;
                    biasY = my / ml * DEPTH_BIAS;
                    biasZ = mz / ml * DEPTH_BIAS;
                }
                frame.batch.quad(
                        ax - frame.camX - biasX, ay - frame.camY - biasY, az - frame.camZ - biasZ,
                        bx - frame.camX - biasX, by - frame.camY - biasY, bz - frame.camZ - biasZ,
                        cx - frame.camX - biasX, cy - frame.camY - biasY, cz - frame.camZ - biasZ,
                        dx - frame.camX - biasX, dy - frame.camY - biasY, dz - frame.camZ - biasZ,
                        color(material, transform, normals, tones, v0, alpha),
                        color(material, transform, normals, tones, v1, alpha),
                        color(material, transform, normals, tones, v2, alpha),
                        color(material, transform, normals, tones, v3, alpha),
                        throughWalls);
            }
        }
    }

    private int color(Material material, Transform transform, float[] normals, float[] tones, int vertexIndex, double alpha) {
        transform.direction(normals[vertexIndex * 3], normals[vertexIndex * 3 + 1], normals[vertexIndex * 3 + 2], normal);
        double light = normal[0] * LIGHT_X + normal[1] * LIGHT_Y + normal[2] * LIGHT_Z;
        return material.color(tones[vertexIndex], light, alpha);
    }

    /** Winding test: the quad is drawn only when its geometric normal points toward the camera. */
    private static boolean facesCamera(double ax, double ay, double az, double bx, double by, double bz,
                                       double cx, double cy, double cz, double mx, double my, double mz) {
        double e1x = bx - ax, e1y = by - ay, e1z = bz - az;
        double e2x = cx - ax, e2y = cy - ay, e2z = cz - az;
        double nx = e1y * e2z - e1z * e2y;
        double ny = e1z * e2x - e1x * e2z;
        double nz = e1x * e2y - e1y * e2x;
        return nx * mx + ny * my + nz * mz < -1e-9;
    }

    private static double transformX(Transform t, float[] p, int v, double s) {
        int i = v * 3;
        return t.originX() + t.rightX() * p[i] * s + t.upX() * p[i + 1] * s + t.forwardX() * p[i + 2] * s;
    }

    private static double transformY(Transform t, float[] p, int v, double s) {
        int i = v * 3;
        return t.originY() + t.rightY() * p[i] * s + t.upY() * p[i + 1] * s + t.forwardY() * p[i + 2] * s;
    }

    private static double transformZ(Transform t, float[] p, int v, double s) {
        int i = v * 3;
        return t.originZ() + t.rightZ() * p[i] * s + t.upZ() * p[i + 1] * s + t.forwardZ() * p[i + 2] * s;
    }

    private void sortDescending() {
        for (int i = 1; i < count; i++) {
            int key = order[i];
            double keyDepth = depth[key];
            int j = i - 1;
            while (j >= 0 && depth[order[j]] < keyDepth) {
                order[j + 1] = order[j];
                j--;
            }
            order[j + 1] = key;
        }
    }
}
