package dev.jilaton.client.render.model.asset;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the registered models are real, closed, outward-facing geometry rather than
 * placeholder boxes. Nothing here needs a renderer: every check is arithmetic on the
 * emitted vertex data.
 */
class ModelAssetGeometryTest {

    private static List<MeshData> allModels() {
        List<MeshData> models = new ArrayList<>();
        for (String id : ModelAssets.knifeIds()) {
            models.add(ModelAssets.knife(id));
        }
        return models;
    }

    @Test
    void everyPartIsFiniteIndexedOutwardFacingGeometry() {
        List<MeshData> models = allModels();
        assertEquals(4, ModelAssets.knifeIds().size(), "four knives");
        for (MeshData model : models) {
            assertFalse(model.parts().isEmpty(), model.id() + " has parts");
            for (MeshPart part : model.parts()) {
                validatePart(model, part);
            }
            validateBounds(model);
        }
    }

    private static void validatePart(MeshData model, MeshPart part) {
        String where = model.id() + "/" + part.material();
        assertTrue(MaterialTags.KNOWN.contains(part.material()), where + ": unknown material tag");
        float[] positions = part.positions();
        float[] normals = part.normals();
        int[] indices = part.indices();
        assertEquals(0, positions.length % 3, where + ": positions are xyz triples");
        assertEquals(positions.length, normals.length, where + ": one normal per position");
        assertEquals(0, indices.length % 3, where + ": indices are triangles");
        int vertexCount = positions.length / 3;
        assertTrue(vertexCount >= 3, where + ": has vertices");
        for (int index : indices) {
            assertTrue(index >= 0 && index < vertexCount, where + ": index " + index + " out of range");
        }

        for (int i = 0; i < positions.length; i += 3) {
            assertTrue(Float.isFinite(positions[i]) && Float.isFinite(positions[i + 1])
                    && Float.isFinite(positions[i + 2]), where + ": non-finite position");
        }
        // The part centroid is the mean of its distinct vertices. For a convex chunk
        // that point is strictly inside the solid, so every face normal must leave it.
        float[] centroid = partCentroid(positions);
        float cx = centroid[0];
        float cy = centroid[1];
        float cz = centroid[2];

        for (int t = 0; t < indices.length; t += 3) {
            int ia = indices[t] * 3;
            int ib = indices[t + 1] * 3;
            int ic = indices[t + 2] * 3;
            float ax = positions[ia], ay = positions[ia + 1], az = positions[ia + 2];
            float bx = positions[ib], by = positions[ib + 1], bz = positions[ib + 2];
            float dx = positions[ic], dy = positions[ic + 1], dz = positions[ic + 2];

            float e1x = bx - ax, e1y = by - ay, e1z = bz - az;
            float e2x = dx - ax, e2y = dy - ay, e2z = dz - az;
            float nxRaw = e1y * e2z - e1z * e2y;
            float nyRaw = e1z * e2x - e1x * e2z;
            float nzRaw = e1x * e2y - e1y * e2x;
            float area2 = (float) Math.sqrt(nxRaw * nxRaw + nyRaw * nyRaw + nzRaw * nzRaw);
            assertTrue(area2 > 1e-9f, where + ": degenerate triangle " + (t / 3) + " (2*area=" + area2 + ")");
            float gx = nxRaw / area2, gy = nyRaw / area2, gz = nzRaw / area2;

            float fx = (ax + bx + dx) / 3f;
            float fy = (ay + by + dy) / 3f;
            float fz = (az + bz + dz) / 3f;

            for (int corner = 0; corner < 3; corner++) {
                int n = (indices[t + corner]) * 3;
                float nx = normals[n], ny = normals[n + 1], nz = normals[n + 2];
                assertTrue(Float.isFinite(nx) && Float.isFinite(ny) && Float.isFinite(nz),
                        where + ": non-finite normal");
                float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                assertEquals(1f, length, 1e-3f, where + ": normal is not unit length");
                // Winding and stored normal must agree (both outward).
                assertTrue(gx * nx + gy * ny + gz * nz > 0.9f,
                        where + ": normal disagrees with triangle winding");
                // ... and that outward direction must face away from the part centre.
                float away = nx * (fx - cx) + ny * (fy - cy) + nz * (fz - cz);
                assertTrue(away > 0f, where + ": normal points toward the part centre (dot=" + away + ")");
            }
        }
    }

    private static float[] partCentroid(float[] positions) {
        Set<String> seen = new HashSet<>();
        double x = 0, y = 0, z = 0;
        int count = 0;
        for (int i = 0; i < positions.length; i += 3) {
            String key = Float.floatToIntBits(positions[i]) + ":" + Float.floatToIntBits(positions[i + 1])
                    + ":" + Float.floatToIntBits(positions[i + 2]);
            if (seen.add(key)) {
                x += positions[i];
                y += positions[i + 1];
                z += positions[i + 2];
                count++;
            }
        }
        return new float[]{(float) (x / count), (float) (y / count), (float) (z / count)};
    }

    private static void validateBounds(MeshData model) {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (MeshPart part : model.parts()) {
            float[] p = part.positions();
            for (int i = 0; i < p.length; i += 3) {
                minX = Math.min(minX, p[i]);
                minY = Math.min(minY, p[i + 1]);
                minZ = Math.min(minZ, p[i + 2]);
                maxX = Math.max(maxX, p[i]);
                maxY = Math.max(maxY, p[i + 1]);
                maxZ = Math.max(maxZ, p[i + 2]);
            }
        }
        assertEquals(minX, model.minX(), 1e-4f, model.id() + " minX");
        assertEquals(minY, model.minY(), 1e-4f, model.id() + " minY");
        assertEquals(minZ, model.minZ(), 1e-4f, model.id() + " minZ");
        assertEquals(maxX, model.maxX(), 1e-4f, model.id() + " maxX");
        assertEquals(maxY, model.maxY(), 1e-4f, model.id() + " maxY");
        assertEquals(maxZ, model.maxZ(), 1e-4f, model.id() + " maxZ");
    }

    @Test
    void knivesAreKnifeSizedRivetedAndGrippedAtTheOrigin() {
        for (String id : ModelAssets.knifeIds()) {
            MeshData knife = ModelAssets.knife(id);
            int triangles = knife.triangleCount();
            assertTrue(triangles >= 300 && triangles <= 1200, id + ": " + triangles + " triangles");

            float overall = Math.max(knife.width(), Math.max(knife.height(), knife.depth()));
            assertTrue(overall >= 0.55f && overall <= 0.75f,
                    id + ": overall length " + overall + " outside 0.55..0.75");

            boolean blade = false;
            boolean edge = false;
            boolean rivet = false;
            float handleMin = Float.POSITIVE_INFINITY;
            float handleMax = Float.NEGATIVE_INFINITY;
            for (MeshPart part : knife.parts()) {
                switch (part.material()) {
                    case "blade" -> blade = true;
                    case "edge" -> edge = true;
                    case "rivet" -> rivet = true;
                    case "handle" -> {
                        float[] p = part.positions();
                        for (int i = 0; i < p.length; i += 3) {
                            handleMin = Math.min(handleMin, p[i]);
                            handleMax = Math.max(handleMax, p[i]);
                        }
                    }
                    default -> {
                    }
                }
            }
            assertTrue(blade && edge, id + ": needs both blade and edge bevel volumes");
            assertTrue(rivet, id + ": needs steel fittings");
            assertTrue(handleMin < Float.POSITIVE_INFINITY, id + ": needs a handle");
            float gripCentre = (handleMin + handleMax) * 0.5f;
            assertTrue(Math.abs(gripCentre) <= 0.03f, id + ": grip centre x=" + gripCentre + " not at the origin");
        }
    }

    @Test
    void idsAreUniqueAndModelsAreNotCopiesOfEachOther() {
        Set<String> ids = new HashSet<>();
        Set<String> shapes = new HashSet<>();
        for (MeshData model : allModels()) {
            assertTrue(ids.add(model.id()), "duplicate id " + model.id());
            String shape = model.triangleCount() + "|" + model.minX() + "|" + model.maxY();
            assertTrue(shapes.add(shape), "two models share a silhouette signature: " + model.id());
        }
        assertEquals(4, ids.size());
    }

    @Test
    void registryResolvesEitherListAndRejectsUnknownIds() {
        assertEquals(ModelAssets.knife("bayonet").id(), ModelAssets.get("bayonet").id());
        // The procedural companions were retired with M29; player characters come from render.character.
        assertThrows(IllegalArgumentException.class, () -> ModelAssets.get("tun_tun_sahur"));
        assertThrows(IllegalArgumentException.class, () -> ModelAssets.get("no_such_model"));
    }

    @Test
    void reportSizesForTheBuildLog() {
        for (MeshData model : allModels()) {
            System.out.println(String.format(Locale.ROOT,
                    "MODEL %-14s parts=%-3d tris=%-4d bounds=[%.3f, %.3f, %.3f]..[%.3f, %.3f, %.3f]",
                    model.id(), model.partCount(), model.triangleCount(),
                    model.minX(), model.minY(), model.minZ(),
                    model.maxX(), model.maxY(), model.maxZ()));
            assertTrue(model.triangleCount() > 0, model.id());
        }
    }
}
