package dev.jilaton.client.render.model;

import dev.jilaton.client.render.model.asset.MeshData;
import dev.jilaton.client.render.model.asset.MeshPart;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bridges the shared asset contract to the quad submit path. {@link MeshData} holds triangles with one
 * material tag per part; {@link Mesh} holds quads with a root-to-tip tone per vertex, so each triangle is
 * submitted as a quad with its last corner repeated. The asset convention puts the blade along +X, while
 * the held pose points along +Z, so model space is rotated by -90 degrees about Y: (x, y, z) becomes
 * (-z, y, x). The spine stays up and the flat of the blade faces sideways.
 */
public final class AssetMesh {

    private AssetMesh() { }

    /**
     * One asset converted for drawing. Extents and {@code length} are in blocks when the fitted model is
     * drawn at scale 1, measured from the model origin (the grip).
     */
    public record Fitted(Map<String, Mesh> parts, double length, double unitsToBlocks,
                         double halfWidth, double halfHeight) {

        public Fitted {
            parts = Collections.unmodifiableMap(new LinkedHashMap<>(parts));
        }

        /** Blocks per asset unit; multiply by the user scale to get the draw scale. */
        public double scaleFor(double userScale) {
            return unitsToBlocks * Math.max(0, userScale);
        }

        /** Lateral half extent once the model is rolled about the forward axis. */
        public double lateralHalf(double rollDegrees) {
            double roll = Math.toRadians(rollDegrees);
            return halfWidth * Math.abs(Math.cos(roll)) + halfHeight * Math.abs(Math.sin(roll));
        }
    }

    /** Fits the model so its longest dimension measures {@code targetLength} at scale 1. */
    public static Fitted fit(MeshData data, double targetLength) {
        double longest = Math.max(data.width(), Math.max(data.height(), data.depth()));
        double units = longest > 1e-4 ? Math.max(1e-4, targetLength) / longest : 1;
        float span = Math.max(1e-4f, data.maxX() - data.minX());
        Map<String, Mesh.Builder> builders = new LinkedHashMap<>();
        Map<String, Integer> offsets = new LinkedHashMap<>();
        for (MeshPart part : data.parts()) {
            Mesh.Builder builder = builders.computeIfAbsent(part.material(), tag -> Mesh.builder());
            int base = offsets.getOrDefault(part.material(), 0);
            float[] positions = part.positions();
            float[] normals = part.normals();
            for (int vertex = 0; vertex < part.vertexCount(); vertex++) {
                int i = vertex * 3;
                builder.vertex(-positions[i + 2], positions[i + 1], positions[i],
                        -normals[i + 2], normals[i + 1], normals[i],
                        Math.max(0f, Math.min(1f, (positions[i] - data.minX()) / span)));
            }
            int[] indices = part.indices();
            for (int t = 0; t + 2 < indices.length; t += 3) {
                int a = base + indices[t];
                int b = base + indices[t + 1];
                int c = base + indices[t + 2];
                builder.quad(a, b, c, c);
            }
            offsets.put(part.material(), base + part.vertexCount());
        }
        Map<String, Mesh> meshes = new LinkedHashMap<>();
        for (Map.Entry<String, Mesh.Builder> entry : builders.entrySet()) {
            meshes.put(entry.getKey(), entry.getValue().build());
        }
        return new Fitted(meshes, longest * units, units,
                Math.max(Math.abs(data.minZ()), Math.abs(data.maxZ())) * units,
                Math.max(Math.abs(data.minY()), Math.abs(data.maxY())) * units);
    }
}
