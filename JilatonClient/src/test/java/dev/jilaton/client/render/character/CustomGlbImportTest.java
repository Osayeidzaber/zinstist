package dev.jilaton.client.render.character;

import dev.jilaton.client.render.character.gltf.CharacterBake;
import dev.jilaton.client.render.character.kit.BakedDesign;
import dev.jilaton.client.render.character.kit.CharacterBuilder;
import dev.jilaton.client.render.character.kit.CharacterDesign;
import dev.jilaton.client.render.character.kit.CharacterModel;
import dev.jilaton.client.render.character.kit.Detail;
import dev.jilaton.client.render.character.kit.PartMesh;
import dev.jilaton.client.render.character.kit.Surface;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomGlbImportTest {
    @TempDir
    Path folder;

    @Test
    void emptyFolderYieldsNoDesigns() {
        assertTrue(CustomCharacters.scan(folder).isEmpty());
    }

    @Test
    void boxGlbBakesIntoAUsableModel() throws IOException {
        Files.write(folder.resolve("alpha_box.glb"), boxGlb(1.0F, 2.0F, 3.0F));

        List<BakedDesign> designs = CustomCharacters.scan(folder);
        assertEquals(1, designs.size());
        assertEquals("alpha_box", designs.get(0).name(), "the design name comes from the file name");

        CharacterModel model = CharacterBuilder.build(designs.get(0), Detail.MEDIUM);
        assertTrue(model.triangles() > 0, "the baked model has no triangles");
        assertTrue(vertices(model) > 0, "the baked model has no vertices");
        assertFinite(model);
        assertExtentOnEveryAxis(model);
        assertTrue(
            Files.exists(folder.resolve(".baked").resolve(designs.get(0).id() + ".anc")),
            "the bake should be cached next to the drop folder"
        );
    }

    @Test
    void twoGlbsAreNamedFromTheirFilesAndBakeIndependently() throws IOException {
        Files.write(folder.resolve("alpha_box.glb"), boxGlb(1.0F, 2.0F, 3.0F));
        Files.write(folder.resolve("beta_spike.glb"), spikeGlb());

        List<BakedDesign> designs = CustomCharacters.scan(folder);
        assertEquals(List.of("alpha_box", "beta_spike"), designs.stream().map(CharacterDesign::name).toList());
        assertNotEquals(designs.get(0).id(), designs.get(1).id(), "two models must not share a cache id");

        for (BakedDesign design : designs) {
            CharacterModel model = CharacterBuilder.build(design, Detail.MEDIUM);
            assertTrue(model.triangles() > 0, design.name() + " baked to nothing");
            assertExtentOnEveryAxis(model);
        }
        assertNotEquals(
            CharacterBuilder.build(designs.get(0), Detail.MEDIUM).triangles(),
            CharacterBuilder.build(designs.get(1), Detail.MEDIUM).triangles(),
            "the two fixtures should not bake to the same mesh"
        );
    }

    @Test
    void secondScanReusesTheBakedCache() throws IOException {
        Path source = folder.resolve("alpha_box.glb");
        Files.write(source, boxGlb(1.0F, 2.0F, 3.0F));

        BakedDesign design = CustomCharacters.scan(folder).get(0);
        assertEquals(design.id(), CustomCharacters.scan(folder).get(0).id(), "the id must be stable between scans");
        CharacterBuilder.build(design, Detail.MEDIUM);

        Path baked = folder.resolve(".baked").resolve(design.id() + ".anc");
        long stamp = Files.isRegularFile(baked) ? Files.getLastModifiedTime(baked).toMillis() : -1L;
        assertTrue(stamp >= 0L, "the first build should have written " + baked);

        Files.delete(source);
        CharacterModel again = CharacterBuilder.build(design, Detail.MEDIUM);
        assertTrue(again.triangles() > 0, "the cached bake should load without the source file");
        assertEquals(stamp, Files.getLastModifiedTime(baked).toMillis(), "a cache hit must not rewrite the bake");
    }

    @Test
    void corruptGlbIsRejectedWithoutBreakingTheScan() throws IOException {
        Files.write(folder.resolve("broken.glb"), new byte[]{0x67, 0x6C, 0x54, 0x46, 2, 0, 0, 0, 0, 0, 0, 0});

        List<BakedDesign> designs = assertDoesNotThrow(() -> CustomCharacters.scan(folder), "scan() must survive a bad file");
        assertEquals(1, designs.size(), "the scan lists the file; the failure belongs to the bake");

        UncheckedIOException failure = assertThrows(
            UncheckedIOException.class, () -> CharacterBuilder.build(designs.get(0), Detail.MEDIUM)
        );
        assertNotNull(failure.getCause());
        assertFalse(failure.getMessage() == null || failure.getMessage().isBlank(), "the failure should name the model");
        assertTrue(failure.getMessage().contains("broken"), failure.getMessage());
    }

    @Test
    void truncatedGlbIsRejectedByTheParser() {
        byte[] good = assertDoesNotThrow(() -> boxGlb(1.0F, 2.0F, 3.0F));
        byte[] cut = Arrays.copyOf(good, good.length / 2);

        IOException failure = assertThrows(
            IOException.class, () -> CharacterBake.bake(cut, new CharacterBake.Options(), null)
        );
        assertFalse(failure.getMessage() == null || failure.getMessage().isBlank(), "the parser should explain the truncation");
    }

    private static int vertices(CharacterModel model) {
        int total = 0;
        for (PartMesh part : model.parts) {
            for (Surface surface : new Surface[]{part.base, part.outline, part.decals}) {
                if (surface != null) {
                    total += surface.vertexCount();
                }
            }
        }
        return total;
    }

    private static void assertFinite(CharacterModel model) {
        for (PartMesh part : model.parts) {
            for (Surface surface : new Surface[]{part.base, part.outline, part.decals}) {
                if (surface == null) {
                    continue;
                }
                assertFinite(part.name, "position", surface.pos);
                assertFinite(part.name, "normal", surface.nrm);
                assertFinite(part.name, "uv", surface.uv);
            }
        }
    }

    private static void assertFinite(String part, String what, float[] values) {
        for (float value : values) {
            assertTrue(Float.isFinite(value), part + " has a non-finite " + what + ": " + value);
        }
    }

    private static void assertExtentOnEveryAxis(CharacterModel model) {
        float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (PartMesh part : model.parts) {
            for (Surface surface : new Surface[]{part.base, part.outline, part.decals}) {
                if (surface == null) {
                    continue;
                }
                for (int i = 0; i < surface.pos.length; i++) {
                    int axis = i % 3;
                    box[axis] = Math.min(box[axis], surface.pos[i]);
                    box[axis + 3] = Math.max(box[axis + 3], surface.pos[i]);
                }
            }
        }
        for (int axis = 0; axis < 3; axis++) {
            assertTrue(
                box[axis + 3] - box[axis] > 1.0E-4F,
                "the baked model is flat on axis " + axis + ": " + box[axis] + " to " + box[axis + 3]
            );
        }
    }

    private static byte[] boxGlb(float sx, float sy, float sz) throws IOException {
        float[] positions = {
            0.0F, 0.0F, 0.0F, sx, 0.0F, 0.0F, sx, sy, 0.0F, 0.0F, sy, 0.0F,
            0.0F, 0.0F, sz, sx, 0.0F, sz, sx, sy, sz, 0.0F, sy, sz
        };
        int[] indices = {
            0, 1, 2, 0, 2, 3,
            4, 6, 5, 4, 7, 6,
            0, 3, 7, 0, 7, 4,
            1, 5, 6, 1, 6, 2,
            0, 4, 5, 0, 5, 1,
            3, 2, 6, 3, 6, 7
        };
        return glb(positions, indices);
    }

    private static byte[] spikeGlb() throws IOException {
        float[] positions = {0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 2.5F, 0.0F, 0.0F, 0.0F, 1.5F};
        int[] indices = {0, 1, 2, 0, 2, 3, 0, 3, 1, 1, 3, 2};
        return glb(positions, indices);
    }

    private static byte[] glb(float[] positions, int[] indices) throws IOException {
        ByteBuffer bin = ByteBuffer.allocate(positions.length * 4 + indices.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : positions) {
            bin.putFloat(value);
        }
        for (int index : indices) {
            bin.putShort((short) index);
        }
        byte[] data = bin.array();

        float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (int i = 0; i < positions.length; i += 3) {
            for (int axis = 0; axis < 3; axis++) {
                min[axis] = Math.min(min[axis], positions[i + axis]);
                max[axis] = Math.max(max[axis], positions[i + axis]);
            }
        }

        String json = "{"
            + "\"asset\":{\"version\":\"2.0\"},"
            + "\"scene\":0,"
            + "\"scenes\":[{\"nodes\":[0]}],"
            + "\"nodes\":[{\"mesh\":0,\"name\":\"Body\"}],"
            + "\"meshes\":[{\"name\":\"Body\",\"primitives\":[{\"attributes\":{\"POSITION\":0},\"indices\":1,\"mode\":4}]}],"
            + "\"buffers\":[{\"byteLength\":" + data.length + "}],"
            + "\"bufferViews\":["
            + "{\"buffer\":0,\"byteOffset\":0,\"byteLength\":" + (positions.length * 4) + ",\"target\":34962},"
            + "{\"buffer\":0,\"byteOffset\":" + (positions.length * 4) + ",\"byteLength\":" + (indices.length * 2) + ",\"target\":34963}"
            + "],"
            + "\"accessors\":["
            + "{\"bufferView\":0,\"componentType\":5126,\"count\":" + (positions.length / 3) + ",\"type\":\"VEC3\","
            + "\"min\":[" + min[0] + "," + min[1] + "," + min[2] + "],\"max\":[" + max[0] + "," + max[1] + "," + max[2] + "]},"
            + "{\"bufferView\":1,\"componentType\":5123,\"count\":" + indices.length + ",\"type\":\"SCALAR\"}"
            + "]"
            + "}";

        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        int jsonPad = (4 - jsonBytes.length % 4) % 4;
        int binPad = (4 - data.length % 4) % 4;
        int total = 12 + 8 + jsonBytes.length + jsonPad + 8 + data.length + binPad;

        ByteBuffer file = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        file.putInt(0x46546C67);
        file.putInt(2);
        file.putInt(total);
        file.putInt(jsonBytes.length + jsonPad);
        file.putInt(0x4E4F534A);
        file.put(jsonBytes);
        for (int i = 0; i < jsonPad; i++) {
            file.put((byte) 0x20);
        }
        file.putInt(data.length + binPad);
        file.putInt(0x004E4942);
        file.put(data);
        for (int i = 0; i < binPad; i++) {
            file.put((byte) 0);
        }
        return file.array();
    }
}
