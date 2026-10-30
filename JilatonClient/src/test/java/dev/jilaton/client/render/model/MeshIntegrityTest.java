package dev.jilaton.client.render.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeshIntegrityTest {
    private static final float EPSILON = 1e-4F;

    @Test
    void everyPrimitiveHasInRangeIndicesAndUnitNormals() {
        Mesh[] meshes = {
                Meshes.box(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5, 0.5F),
                Meshes.strip(6, 0.055, 0.01, 0.045, -0.06),
                Meshes.torus(0.3, 0.02, 32, 5),
                Meshes.diamond(0.12, 0.16, -0.10)
        };
        for (Mesh mesh : meshes) assertWellFormed(mesh);
    }

    @Test
    void stripMatchesTheRequestedSegmentCountAndIsDoubleSided() {
        Mesh strip = Meshes.strip(5, 0.055, 0.01, 0.045, -0.06);
        assertEquals(5 * 4, strip.quadCount(), "four quads per segment: two halves on two sides");
        boolean hasPositive = false, hasNegative = false;
        for (int i = 0; i < strip.vertexCount(); i++) {
            float ny = strip.normals()[i * 3 + 1];
            hasPositive |= ny > 0.05F;
            hasNegative |= ny < -0.05F;
        }
        assertTrue(hasPositive && hasNegative, "a feather needs faces pointing both ways to survive culling");
    }

    @Test
    void torusStaysInsideItsRadiusBounds() {
        Mesh torus = Meshes.torus(0.3, 0.02, 24, 4);
        double outer = 0.32;
        for (int i = 0; i < torus.vertexCount(); i++) {
            double x = torus.positions()[i * 3], y = torus.positions()[i * 3 + 1], z = torus.positions()[i * 3 + 2];
            assertTrue(Math.abs(Math.sqrt(x * x + z * z) - 0.3) <= outer - 0.3 + EPSILON);
            assertTrue(Math.abs(y) <= 0.02 + EPSILON);
        }
    }

    @Test
    void builderRejectsOutOfRangeIndices() {
        Mesh.Builder builder = Mesh.builder();
        int a = builder.vertex(0, 0, 0, 0, 1, 0, 0);
        int b = builder.vertex(1, 0, 0, 0, 1, 0, 0);
        int c = builder.vertex(1, 0, 1, 0, 1, 0, 0);
        builder.quad(a, b, c, 99);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void builderRejectsZeroAndNonFiniteNormals() {
        Mesh.Builder zero = Mesh.builder();
        assertThrows(IllegalArgumentException.class, () -> zero.vertex(0, 0, 0, 0, 0, 0, 0));
        Mesh.Builder notFinite = Mesh.builder();
        assertThrows(IllegalArgumentException.class, () -> notFinite.vertex(0, 0, 0, Float.NaN, 0, 1, 0));
    }

    @Test
    void builderNormalisesOversizedNormals() {
        Mesh.Builder builder = Mesh.builder();
        builder.vertex(0, 0, 0, 0, 0, 12, 0);
        Mesh mesh = builder.build();
        assertEquals(1, mesh.vertexCount());
        assertEquals(1F, mesh.normals()[2], 1e-5F);
    }

    private static void assertWellFormed(Mesh mesh) {
        int vertices = mesh.vertexCount();
        int[] indices = mesh.indices();
        assertEquals(mesh.quadCount() * 4, indices.length);
        assertTrue(mesh.quadCount() > 0, "primitive produced no faces");
        for (int index : indices) assertTrue(index >= 0 && index < vertices, "index outside vertex range");
        for (int i = 0; i < vertices; i++) {
            float x = mesh.positions()[i * 3], y = mesh.positions()[i * 3 + 1], z = mesh.positions()[i * 3 + 2];
            assertTrue(Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z), "non-finite position");
            float nx = mesh.normals()[i * 3], ny = mesh.normals()[i * 3 + 1], nz = mesh.normals()[i * 3 + 2];
            assertEquals(1F, (float) Math.sqrt(nx * nx + ny * ny + nz * nz), 2e-3F, "normal is not unit length");
            assertTrue(mesh.tones()[i] >= 0 && mesh.tones()[i] <= 1, "tone outside 0..1");
        }
    }
}
