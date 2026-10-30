package dev.jilaton.client.render.model;

import dev.jilaton.client.render.model.asset.MeshData;
import dev.jilaton.client.render.model.asset.MeshPart;
import dev.jilaton.client.render.model.asset.ModelAssets;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetMeshTest {
    private static final double TARGET = 0.34;

    @Test
    void everyKnifeConvertsIntoTriangleBackedQuads() {
        for (String id : ModelAssets.knifeIds()) {
            MeshData data = ModelAssets.knife(id);
            AssetMesh.Fitted fitted = AssetMesh.fit(data, TARGET);
            int quads = 0;
            for (Mesh mesh : fitted.parts().values()) {
                quads += mesh.quadCount();
                assertEquals(mesh.quadCount() * 4, mesh.indices().length, id + " quad storage is ragged");
                for (int index : mesh.indices()) {
                    assertTrue(index >= 0 && index < mesh.vertexCount(), id + " index out of range");
                }
                for (int vertex = 0; vertex < mesh.vertexCount(); vertex++) {
                    float nx = mesh.normals()[vertex * 3];
                    float ny = mesh.normals()[vertex * 3 + 1];
                    float nz = mesh.normals()[vertex * 3 + 2];
                    assertEquals(1F, (float) Math.sqrt(nx * nx + ny * ny + nz * nz), 2e-3F,
                            id + " has a non-unit normal");
                    float tone = mesh.tones()[vertex];
                    assertTrue(tone >= 0f && tone <= 1f, id + " tone out of range: " + tone);
                }
            }
            assertEquals(data.triangleCount(), quads, id + " lost or invented triangles");
        }
    }

    @Test
    void materialTagsSurviveTheConversion() {
        String id = "bayonet";
        MeshData data = ModelAssets.knife(id);
        Set<String> expected = new LinkedHashSet<>();
        for (MeshPart part : data.parts()) expected.add(part.material());
        AssetMesh.Fitted fitted = AssetMesh.fit(data, TARGET);
        assertEquals(expected, fitted.parts().keySet(), "material split changed in the adapter");
        assertTrue(fitted.parts().containsKey("blade"));
        assertTrue(fitted.parts().containsKey("edge"));
        assertTrue(fitted.parts().containsKey("handle"));
    }

    @Test
    void fitNormalisesTheLongestDimension() {
        for (String id : ModelAssets.knifeIds()) {
            MeshData data = ModelAssets.knife(id);
            AssetMesh.Fitted fitted = AssetMesh.fit(data, TARGET);
            assertEquals(TARGET, fitted.length(), 1e-6, id + " is not fitted to the target length");
            double longest = Math.max(data.width(), Math.max(data.height(), data.depth()));
            assertEquals(TARGET / longest, fitted.unitsToBlocks(), 1e-9);
        }
    }

    @Test
    void bladeAxisMapsToTheForwardAxisForEveryVariant() {
        for (String id : ModelAssets.knifeIds()) {
            MeshData data = ModelAssets.knife(id);
            AssetMesh.Fitted fitted = AssetMesh.fit(data, TARGET);
            double[] bounds = bounds(fitted.parts().values());
            double spanX = bounds[3] - bounds[0];
            double spanY = bounds[4] - bounds[1];
            double spanZ = bounds[5] - bounds[2];
            // Meshes stay in asset units; ModelBatch applies unitsToBlocks at draw time.
            assertEquals(data.depth(), spanX, 1e-5, id + " thickness should map to X");
            assertEquals(data.height(), spanY, 1e-5, id + " height should stay on Y");
            assertEquals(data.width(), spanZ, 1e-5, id + " blade should point along +Z");
            assertTrue(spanZ > spanX && spanZ > spanY, id + " is longer than it is thick or tall");
        }
    }

    @Test
    void everyVariantConvertsToFiniteNonDegenerateGeometry() {
        for (String id : ModelAssets.knifeIds()) {
            AssetMesh.Fitted fitted = AssetMesh.fit(ModelAssets.knife(id), TARGET);
            assertFalse(fitted.parts().isEmpty(), id + " produced no parts");
            double[] bounds = bounds(fitted.parts().values());
            for (int axis = 0; axis < 3; axis++) {
                assertTrue(Double.isFinite(bounds[axis]) && Double.isFinite(bounds[axis + 3]),
                        id + " has a non-finite bound on axis " + axis);
                assertTrue(bounds[axis + 3] - bounds[axis] > 1e-4, id + " is flat on axis " + axis);
            }
            for (Mesh mesh : fitted.parts().values()) {
                for (float value : mesh.positions()) assertTrue(Float.isFinite(value), id + " position NaN");
                for (float value : mesh.normals()) assertTrue(Float.isFinite(value), id + " normal NaN");
                for (float value : mesh.tones()) assertTrue(Float.isFinite(value), id + " tone NaN");
            }
        }
    }

    @Test
    void theAdapterPreservesEveryVariantsDimensions() {
        for (String id : ModelAssets.knifeIds()) {
            MeshData data = ModelAssets.knife(id);
            AssetMesh.Fitted fitted = AssetMesh.fit(data, TARGET);
            double[] bounds = bounds(fitted.parts().values());
            assertEquals(data.width(), bounds[5] - bounds[2], 1e-5, id + " width changed");
            assertEquals(data.height(), bounds[4] - bounds[1], 1e-5, id + " height changed");
            assertEquals(data.depth(), bounds[3] - bounds[0], 1e-5, id + " depth changed");
        }
    }

    @Test
    void lateralExtentAccountsForARolledBlade() {
        AssetMesh.Fitted fitted = AssetMesh.fit(ModelAssets.knife("bayonet"), TARGET);
        assertEquals(fitted.halfWidth(), fitted.lateralHalf(0), 1e-12);
        assertTrue(fitted.lateralHalf(45) > fitted.lateralHalf(0),
                "rolling a tall blade presents more of it sideways");
        assertEquals(fitted.halfHeight(), fitted.lateralHalf(90), 1e-12);
    }

    @Test
    void theBladeGradientRunsFromGripToTip() {
        Mesh blade = AssetMesh.fit(ModelAssets.knife("bayonet"), TARGET).parts().get("blade");
        float min = 1, max = 0;
        for (float tone : blade.tones()) {
            min = Math.min(min, tone);
            max = Math.max(max, tone);
        }
        assertTrue(max > 0.9f, "the blade tip should reach the light end of the gradient");
        assertTrue(min < 0.5f, "the blade root should stay in the dark half of the gradient");
    }

    @Test
    void fittedExtentsStaySmallerThanTheDrawnLength() {
        for (String id : ModelAssets.knifeIds()) {
            AssetMesh.Fitted fitted = AssetMesh.fit(ModelAssets.knife(id), TARGET);
            assertTrue(fitted.halfWidth() <= fitted.length() * 0.5 + 1e-9, id + " half width is impossible");
            assertTrue(fitted.halfHeight() <= fitted.length() * 0.5 + 1e-9, id + " half height is impossible");
            assertFalse(fitted.parts().isEmpty());
        }
    }

    private static double[] bounds(Iterable<Mesh> meshes) {
        double[] b = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE,
                -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (Mesh mesh : meshes) {
            float[] p = mesh.positions();
            for (int i = 0; i < p.length; i += 3) {
                b[0] = Math.min(b[0], p[i]);
                b[1] = Math.min(b[1], p[i + 1]);
                b[2] = Math.min(b[2], p[i + 2]);
                b[3] = Math.max(b[3], p[i]);
                b[4] = Math.max(b[4], p[i + 1]);
                b[5] = Math.max(b[5], p[i + 2]);
            }
        }
        return b;
    }
}
