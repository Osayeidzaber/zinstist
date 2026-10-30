package dev.jilaton.client.render.model;

import java.util.Arrays;

/**
 * Immutable quad mesh generated in code: one position, one unit normal and one gradient coordinate per
 * vertex, plus indices in groups of four. The world submit path consumes quads directly, so no triangle
 * expansion or texture coordinates exist here. Meshes are shared between frames and never mutated.
 */
public final class Mesh {
    private final float[] positions;
    private final float[] normals;
    private final float[] tones;
    private final int[] indices;
    private final double centreX, centreY, centreZ;

    private Mesh(float[] positions, float[] normals, float[] tones, int[] indices) {
        this.positions = positions;
        this.normals = normals;
        this.tones = tones;
        this.indices = indices;
        double sx = 0, sy = 0, sz = 0;
        int vertices = positions.length / 3;
        for (int i = 0; i < vertices; i++) {
            sx += positions[i * 3];
            sy += positions[i * 3 + 1];
            sz += positions[i * 3 + 2];
        }
        centreX = vertices == 0 ? 0 : sx / vertices;
        centreY = vertices == 0 ? 0 : sy / vertices;
        centreZ = vertices == 0 ? 0 : sz / vertices;
    }

    public static Builder builder() { return new Builder(); }

    public int vertexCount() { return positions.length / 3; }
    public int quadCount() { return indices.length / 4; }
    /** Internal storage, read-only by contract. */
    public float[] positions() { return positions; }
    public float[] normals() { return normals; }
    public float[] tones() { return tones; }
    public int[] indices() { return indices; }
    public double centreX() { return centreX; }
    public double centreY() { return centreY; }
    public double centreZ() { return centreZ; }

    /**
     * Builds one mesh. Normals are normalised on entry so a caller cannot smuggle a non-unit normal into
     * the submit path; the build check is the last line of defence before geometry reaches the batch.
     */
    public static final class Builder {
        private float[] positions = new float[48];
        private float[] normals = new float[48];
        private float[] tones = new float[16];
        private int[] indices = new int[32];
        private int vertices;
        private int quads;

        public int vertex(float x, float y, float z, float nx, float ny, float nz, float tone) {
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
                throw new IllegalArgumentException("Mesh position must be finite");
            }
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (!(length > 1e-6F) || !Float.isFinite(length)) {
                throw new IllegalArgumentException("Mesh normal must be a finite non-zero vector");
            }
            if (!Float.isFinite(tone)) throw new IllegalArgumentException("Mesh tone must be finite");
            ensureVertices(vertices + 1);
            int base = vertices * 3;
            positions[base] = x;
            positions[base + 1] = y;
            positions[base + 2] = z;
            normals[base] = nx / length;
            normals[base + 1] = ny / length;
            normals[base + 2] = nz / length;
            tones[vertices] = tone;
            return vertices++;
        }

        /** Adds one quad from four vertex indices, wound counter-clockwise around its front face. */
        public void quad(int a, int b, int c, int d) {
            ensureIndices((quads + 1) * 4);
            int base = quads * 4;
            indices[base] = a;
            indices[base + 1] = b;
            indices[base + 2] = c;
            indices[base + 3] = d;
            quads++;
        }

        public int quadCount() { return quads; }

        private void ensureVertices(int wanted) {
            int needed = wanted * 3;
            if (needed <= positions.length) return;
            int capacity = Math.max(needed, positions.length * 2);
            positions = Arrays.copyOf(positions, capacity);
            normals = Arrays.copyOf(normals, capacity);
            tones = Arrays.copyOf(tones, Math.max(wanted, tones.length * 2));
        }

        private void ensureIndices(int wanted) {
            if (wanted <= indices.length) return;
            indices = Arrays.copyOf(indices, Math.max(wanted, indices.length * 2));
        }

        public Mesh build() {
            for (int i = 0; i < quads * 4; i++) {
                int index = indices[i];
                if (index < 0 || index >= vertices) {
                    throw new IllegalStateException("Quad index " + index + " is outside 0.." + (vertices - 1));
                }
            }
            float[] p = Arrays.copyOf(positions, vertices * 3);
            float[] n = Arrays.copyOf(normals, vertices * 3);
            float[] t = Arrays.copyOf(tones, vertices);
            int[] i = Arrays.copyOf(indices, quads * 4);
            return new Mesh(p, n, t, i);
        }
    }
}
