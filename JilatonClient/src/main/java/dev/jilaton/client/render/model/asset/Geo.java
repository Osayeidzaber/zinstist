package dev.jilaton.client.render.model.asset;

import java.util.ArrayList;
import java.util.List;

/** Small vector and curve helpers used by the model builders. */
final class Geo {

    static final float[] X = {1f, 0f, 0f};
    static final float[] Y = {0f, 1f, 0f};
    static final float[] Z = {0f, 0f, 1f};

    private Geo() {
    }

    static float[] of(float x, float y, float z) {
        return new float[]{x, y, z};
    }

    static float[] add(float[] a, float[] b) {
        return new float[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    static float[] sub(float[] a, float[] b) {
        return new float[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static float[] scale(float[] a, float s) {
        return new float[]{a[0] * s, a[1] * s, a[2] * s};
    }

    static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static float[] cross(float[] a, float[] b) {
        return new float[]{
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]};
    }

    static float length(float[] a) {
        return (float) Math.sqrt(dot(a, a));
    }

    static float[] normalise(float[] a) {
        float len = length(a);
        if (len < 1e-9f) {
            throw new IllegalArgumentException("cannot normalise a zero-length vector");
        }
        return scale(a, 1f / len);
    }

    /** Quadratic Bezier point at {@code t} in [0, 1]. */
    static float[] bezier(float[] p0, float[] p1, float[] p2, float t) {
        float u = 1f - t;
        return new float[]{
                u * u * p0[0] + 2f * u * t * p1[0] + t * t * p2[0],
                u * u * p0[1] + 2f * u * t * p1[1] + t * t * p2[1],
                u * u * p0[2] + 2f * u * t * p1[2] + t * t * p2[2]};
    }

    /** Un-normalised derivative of the quadratic Bezier at {@code t}. */
    static float[] bezierTangent(float[] p0, float[] p1, float[] p2, float t) {
        float u = 1f - t;
        return new float[]{
                2f * u * (p1[0] - p0[0]) + 2f * t * (p2[0] - p1[0]),
                2f * u * (p1[1] - p0[1]) + 2f * t * (p2[1] - p1[1]),
                2f * u * (p1[2] - p0[2]) + 2f * t * (p2[2] - p1[2])};
    }

    /**
     * A closed ring of {@code sides} points around {@code centre}; {@code up}/{@code side}
     * span the ring plane and must be perpendicular to each other.
     */
    static float[][] frameRing(float[] centre, float[] up, float[] side,
                               float radiusUp, float radiusSide, int sides, float phase) {
        float[][] ring = new float[sides][];
        for (int i = 0; i < sides; i++) {
            double angle = phase + 2.0 * Math.PI * i / sides;
            float c = (float) Math.cos(angle);
            float s = (float) Math.sin(angle);
            ring[i] = new float[]{
                    centre[0] + up[0] * c * radiusUp + side[0] * s * radiusSide,
                    centre[1] + up[1] * c * radiusUp + side[1] * s * radiusSide,
                    centre[2] + up[2] * c * radiusUp + side[2] * s * radiusSide};
        }
        return ring;
    }

    /** A circular ring perpendicular to an arbitrary axis. */
    static float[][] axisRing(float[] centre, float[] axis, float radius, int sides, float phase) {
        float[] ref = Math.abs(axis[1]) > 0.9f ? X : Y;
        float[] u = normalise(cross(axis, ref));
        float[] v = normalise(cross(axis, u));
        return frameRing(centre, u, v, radius, radius, sides, phase);
    }

    /** Rings of increasing radius along a straight axis, starting at {@code start}. */
    static List<float[][]> axisRings(float[] start, float[] direction,
                                     float[] distances, float[] radii, int sides, float phase) {
        float[] axis = normalise(direction);
        float[] up = cross(axis, Z);
        if (length(up) < 1e-4f) {
            up = X;
        }
        up = normalise(up);
        float[] side = normalise(cross(axis, up));
        List<float[][]> rings = new ArrayList<>();
        for (int i = 0; i < distances.length; i++) {
            rings.add(frameRing(add(start, scale(axis, distances[i])),
                    up, side, radii[i], radii[i], sides, phase));
        }
        return rings;
    }

    /**
     * A rectangle with cut corners, lying in the YZ plane at {@code x}: the cross
     * section used for guards, bodies and muzzles.
     */
    static float[][] roundedRectX(float x, float centreY, float centreZ,
                                  float halfY, float halfZ, float cut) {
        if (cut <= 0f || cut >= Math.min(halfY, halfZ)) {
            throw new IllegalArgumentException("corner cut must be positive and smaller than both half extents");
        }
        return new float[][]{
                of(x, centreY + halfY, centreZ + halfZ - cut),
                of(x, centreY + halfY - cut, centreZ + halfZ),
                of(x, centreY - halfY + cut, centreZ + halfZ),
                of(x, centreY - halfY, centreZ + halfZ - cut),
                of(x, centreY - halfY, centreZ - halfZ + cut),
                of(x, centreY - halfY + cut, centreZ - halfZ),
                of(x, centreY + halfY - cut, centreZ - halfZ),
                of(x, centreY + halfY, centreZ - halfZ + cut),
        };
    }
}
