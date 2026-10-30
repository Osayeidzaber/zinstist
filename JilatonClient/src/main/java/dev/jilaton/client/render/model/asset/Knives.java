package dev.jilaton.client.render.model.asset;

import java.util.ArrayList;
import java.util.List;

/**
 * The four knife silhouettes. Every one is a closed solid built from lofted cross
 * sections: a blade volume split along the grind line into a bright face and a distinct
 * edge bevel, a guard, a faceted grip and steel fittings. Each is about two thirds of a
 * block long with the grip centred on the model origin.
 */
final class Knives {

    /**
     * Blade section in (spine, thickness) space: a flattened diamond with two shoulders
     * and a narrow bevel band around the cutting edge. Index 3 is the edge itself.
     */
    private static final float[][] SECTION = {
            { 1.00f,  0.00f},
            { 0.32f,  0.85f},
            {-0.55f,  0.32f},
            {-1.00f,  0.00f},
            {-0.55f, -0.32f},
            { 0.32f, -0.85f},
    };
    /** Everything above the grind line: the blade face. */
    private static final int[] FACE_SIDE = {0, 1, 2, 4, 5};
    /** Everything below it: the edge bevel. */
    private static final int[] EDGE_SIDE = {2, 3, 4};

    private Knives() {
    }

    // ------------------------------------------------------------------ bayonet

    static MeshData bayonet() {
        ModelBuilder m = new ModelBuilder("bayonet");

        List<float[][]> rings = new ArrayList<>();
        addRingX(rings, 0.125f, 0.000f, 0.036f, 0.0085f);
        addRingX(rings, 0.200f, 0.000f, 0.035f, 0.0080f);
        addRingX(rings, 0.290f, -0.001f, 0.031f, 0.0072f);
        addRingX(rings, 0.370f, -0.002f, 0.026f, 0.0060f);
        addRingX(rings, 0.430f, -0.004f, 0.018f, 0.0042f);
        addRingX(rings, 0.462f, -0.006f, 0.011f, 0.0026f);
        straightBlade(m, rings, Geo.of(0.500f, -0.010f, 0f));

        // Hexagonal cross guard, thicker than the blade is wide.
        m.part("rivet").prism(Geo.roundedRectX(0.098f, 0f, 0f, 0.056f, 0.016f, 0.008f),
                Geo.of(0.028f, 0f, 0f));

        float[] gripX = {-0.115f, -0.095f, -0.065f, -0.035f, -0.005f, 0.030f, 0.065f, 0.096f};
        float[] gripR = {0.0205f, 0.0215f, 0.0182f, 0.0212f, 0.0182f, 0.0212f, 0.0182f, 0.0200f};
        List<float[][]> grip = new ArrayList<>();
        for (int i = 0; i < gripX.length; i++) {
            grip.add(Geo.frameRing(Geo.of(gripX[i], 0f, 0f), Geo.Y, Geo.Z, gripR[i], gripR[i], 10, 0f));
        }
        m.part("handle").loft(grip, true, true);

        for (float rivetX : new float[]{-0.075f, 0.0f, 0.075f}) {
            throughPin(m, rivetX, 0.0f, 0.0f, 0.0034f, 0.024f, 8);
        }
        m.part("rivet").loft(Geo.axisRings(Geo.of(-0.137f, 0f, 0f), Geo.X,
                new float[]{0f, 0.022f}, new float[]{0.0235f, 0.0235f}, 8, 0f), true, true);

        return m.build();
    }

    // ----------------------------------------------------------------- karambit

    static MeshData karambit() {
        ModelBuilder m = new ModelBuilder("karambit");

        // Grip curls down and back from the guard; split into convex chunks.
        float[] handleStart = Geo.of(0.165f, 0.000f, 0f);
        float[] handleControl = Geo.of(0.000f, 0.030f, 0f);
        float[] handleEnd = Geo.of(-0.115f, -0.080f, 0f);
        float[] handleRadii = {0.0205f, 0.0185f, 0.0210f, 0.0188f, 0.0205f, 0.0190f, 0.0198f};
        int handleSegments = 6;
        List<float[][]> handleRings = new ArrayList<>();
        for (int i = 0; i <= handleSegments; i++) {
            float t = (float) i / handleSegments;
            float[] centre = Geo.bezier(handleStart, handleControl, handleEnd, t);
            float[] tangent = Geo.normalise(Geo.bezierTangent(handleStart, handleControl, handleEnd, t));
            float[] up = Geo.normalise(Geo.cross(Geo.Z, tangent));
            handleRings.add(Geo.frameRing(centre, up, Geo.Z, handleRadii[i], handleRadii[i], 8, 0f));
        }
        segmentedTube(m, "handle", handleRings);
        throughPin(m, 0.080f, 0.015f, 0.0f, 0.0030f, 0.022f, 8);

        // Finger ring: ten convex bars on a circle, tucked over the grip's end.
        float[] ringCentre = Geo.of(-0.155f, -0.095f, 0f);
        for (int i = 0; i < 10; i++) {
            double angle = 2.0 * Math.PI * i / 10;
            float[] radial = Geo.of((float) Math.cos(angle), (float) Math.sin(angle), 0f);
            float[] tangential = Geo.of(-(float) Math.sin(angle), (float) Math.cos(angle), 0f);
            m.part("handle").orientedBox(Geo.add(ringCentre, Geo.scale(radial, 0.045f)),
                    radial, tangential, Geo.Z, 0.010f, 0.021f, 0.009f);
        }

        // Talon: a 90 degree hook whose spine follows the outside of the curve.
        float[] bladeStart = Geo.of(0.175f, 0.005f, 0f);
        float[] bladeControl = Geo.of(0.365f, 0.010f, 0f);
        float[] bladeEnd = Geo.of(0.400f, 0.235f, 0f);
        List<float[][]> bladeRings = new ArrayList<>();
        for (int i = 0; i <= 9; i++) {
            float u = i / 10f;
            float[] centre = Geo.bezier(bladeStart, bladeControl, bladeEnd, u);
            float[] tangent = Geo.normalise(Geo.bezierTangent(bladeStart, bladeControl, bladeEnd, u));
            float[] outward = Geo.normalise(Geo.cross(Geo.Z, tangent));
            float spineHeight = 0.0035f + 0.0385f * (float) Math.pow(1f - u, 1.2);
            float thickness = 0.0012f + 0.0073f * (1f - u);
            bladeRings.add(sectionRing(centre, Geo.scale(outward, -1f), Geo.Z, spineHeight, thickness));
        }
        segmentedBlade(m, bladeRings, bladeEnd);

        // Bolster where grip and blade meet.
        m.part("rivet").orientedBox(Geo.of(0.168f, 0.000f, 0f), Geo.X, Geo.Y, Geo.Z,
                0.016f, 0.045f, 0.019f);

        return m.build();
    }

    // ----------------------------------------------------------------- butterfly

    static MeshData butterfly() {
        ModelBuilder m = new ModelBuilder("butterfly");

        List<float[][]> rings = new ArrayList<>();
        addRingX(rings, 0.155f, 0.000f, 0.030f, 0.0060f);
        addRingX(rings, 0.235f, 0.001f, 0.034f, 0.0055f);
        addRingX(rings, 0.315f, 0.000f, 0.034f, 0.0050f);
        addRingX(rings, 0.390f, -0.001f, 0.029f, 0.0040f);
        addRingX(rings, 0.455f, -0.003f, 0.017f, 0.0025f);
        addRingX(rings, 0.505f, -0.005f, 0.004f, 0.0009f);
        straightBlade(m, rings, null);

        // Two handles, splayed either side of the pivot and swept slightly back.
        for (float side : new float[]{-1f, 1f}) {
            float[] h0 = Geo.of(0.145f, 0.000f, 0.0135f * side);
            float[] h1 = Geo.of(0.000f, 0.020f, 0.0160f * side);
            float[] h2 = Geo.of(-0.160f, -0.030f, 0.0210f * side);
            int segments = 6;
            List<float[][]> handleRings = new ArrayList<>();
            for (int i = 0; i <= segments; i++) {
                float t = (float) i / segments;
                float[] centre = Geo.bezier(h0, h1, h2, t);
                float[] tangent = Geo.normalise(Geo.bezierTangent(h0, h1, h2, t));
                float[] up = Geo.normalise(Geo.cross(Geo.Z, tangent));
                float radius = 0.0165f - 0.0015f * (i % 2);
                handleRings.add(Geo.frameRing(centre, up, Geo.Z, radius, 0.0095f, 10, 0f));
            }
            segmentedTube(m, "handle", handleRings);
        }

        throughPin(m, 0.145f, 0.0f, 0.0f, 0.0075f, 0.030f, 8);
        for (float side : new float[]{-1f, 1f}) {
            m.part("rivet").loft(Geo.axisRings(Geo.of(-0.150f, -0.032f, 0.021f * side), Geo.Z,
                    new float[]{-0.010f, 0.010f}, new float[]{0.0050f, 0.0050f}, 6, 0f), true, true);
        }

        return m.build();
    }

    // --------------------------------------------------------------------- tanto

    static MeshData tanto() {
        ModelBuilder m = new ModelBuilder("tanto");

        // Straight spine, wide bevel, and an angular kissaki that drops to a point at
        // the edge line instead of tapering symmetrically.
        List<float[][]> rings = new ArrayList<>();
        addRingX(rings, 0.115f, 0.000f, 0.050f, 0.0105f);
        addRingX(rings, 0.190f, 0.000f, 0.050f, 0.0100f);
        addRingX(rings, 0.265f, -0.001f, 0.049f, 0.0092f);
        addRingX(rings, 0.330f, -0.002f, 0.047f, 0.0080f);
        addRingX(rings, 0.375f, -0.003f, 0.044f, 0.0068f);
        straightBlade(m, rings, Geo.of(0.462f, -0.040f, 0f));

        m.part("rivet").prism(Geo.roundedRectX(0.098f, 0f, 0f, 0.058f, 0.021f, 0.009f),
                Geo.of(0.028f, 0f, 0f));

        float[] gripX = {-0.115f, -0.090f, -0.055f, -0.020f, 0.015f, 0.050f, 0.080f, 0.100f};
        float[] gripR = {0.0210f, 0.0235f, 0.0205f, 0.0235f, 0.0205f, 0.0235f, 0.0205f, 0.0210f};
        List<float[][]> grip = new ArrayList<>();
        for (int i = 0; i < gripX.length; i++) {
            grip.add(Geo.frameRing(Geo.of(gripX[i], 0f, 0f), Geo.Y, Geo.Z, gripR[i], gripR[i], 12, 0f));
        }
        m.part("handle").loft(grip, true, true);

        for (float rivetX : new float[]{-0.095f, -0.055f, -0.015f, 0.045f}) {
            throughPin(m, rivetX, 0.0f, 0.0f, 0.0032f, 0.026f, 8);
        }
        m.part("rivet").loft(Geo.axisRings(Geo.of(-0.140f, 0f, 0f), Geo.X,
                new float[]{0f, 0.025f}, new float[]{0.0240f, 0.0235f}, 8, 0f), true, true);

        return m.build();
    }

    // ------------------------------------------------------------------ helpers

    private static void addRingX(List<float[][]> rings, float x, float centreY, float height, float thickness) {
        rings.add(sectionRing(Geo.of(x, centreY, 0f), Geo.Y, Geo.Z, height, thickness));
    }

    private static float[][] sectionRing(float[] centre, float[] spine, float[] side,
                                         float height, float thickness) {
        float[][] ring = new float[SECTION.length][];
        for (int i = 0; i < SECTION.length; i++) {
            ring[i] = Geo.add(centre, Geo.add(
                    Geo.scale(spine, SECTION[i][0] * height),
                    Geo.scale(side, SECTION[i][1] * thickness)));
        }
        return ring;
    }

    private static float[][] pick(float[][] ring, int[] indices) {
        float[][] out = new float[indices.length][];
        for (int i = 0; i < indices.length; i++) {
            out[i] = ring[indices[i]];
        }
        return out;
    }

    private static void straightBlade(ModelBuilder m, List<float[][]> rings, float[] apex) {
        List<float[][]> faceSide = new ArrayList<>();
        List<float[][]> edgeSide = new ArrayList<>();
        for (float[][] ring : rings) {
            faceSide.add(pick(ring, FACE_SIDE));
            edgeSide.add(pick(ring, EDGE_SIDE));
        }
        m.part("blade").loft(faceSide, true, apex == null);
        m.part("edge").loft(edgeSide, true, apex == null);
        if (apex != null) {
            m.part("blade").apexFan(faceSide.get(faceSide.size() - 1), apex);
            m.part("edge").apexFan(edgeSide.get(edgeSide.size() - 1), apex);
        }
    }

    /** Same as {@link #straightBlade} but every segment is its own convex part. */
    private static void segmentedBlade(ModelBuilder m, List<float[][]> rings, float[] apex) {
        int segments = rings.size() - 1;
        for (int i = 0; i < segments; i++) {
            boolean first = i == 0;
            boolean last = i == segments - 1;
            float[][] faceA = pick(rings.get(i), FACE_SIDE);
            float[][] faceB = pick(rings.get(i + 1), FACE_SIDE);
            float[][] edgeA = pick(rings.get(i), EDGE_SIDE);
            float[][] edgeB = pick(rings.get(i + 1), EDGE_SIDE);
            m.part("blade").loft(List.of(faceA, faceB), first, last && apex == null);
            m.part("edge").loft(List.of(edgeA, edgeB), first, last && apex == null);
            if (last && apex != null) {
                m.part("blade").apexFan(faceB, apex);
                m.part("edge").apexFan(edgeB, apex);
            }
        }
    }

    private static void segmentedTube(ModelBuilder m, String material, List<float[][]> rings) {
        int segments = rings.size() - 1;
        for (int i = 0; i < segments; i++) {
            m.part(material).loft(List.of(rings.get(i), rings.get(i + 1)),
                    i == 0, i == segments - 1);
        }
    }

    /** Pin through the grip along Z, protruding on both sides. */
    private static void throughPin(ModelBuilder m, float x, float y, float centreZ,
                                   float radius, float halfLength, int sides) {
        m.part("rivet").loft(Geo.axisRings(Geo.of(x, y, centreZ), Geo.Z,
                new float[]{-halfLength, halfLength},
                new float[]{radius, radius}, sides, 0f), true, true);
    }
}
