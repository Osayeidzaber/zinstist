package dev.jilaton.client.render.character;

import dev.jilaton.client.render.character.kit.Bone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterRotationTableTest {
    private static final int LEVELS = 17;
    private static final float TOLERANCE = 1.0E-5F;

    @Test
    void tableHoldsSeventeenNineFloatRotations() {
        assertEquals(LEVELS * 9, new CharacterRotationTable().values.length);
    }

    @Test
    void levelZeroIsTheIdentity() {
        CharacterRotationTable table = new CharacterRotationTable();
        table.update(0.7F, -1.1F, 0.4F);
        assertMatrix(table.values, 0, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F);
    }

    @Test
    void everyLevelStaysOrthonormalAndRightHanded() {
        CharacterRotationTable table = new CharacterRotationTable();
        table.update(0.63F, -1.27F, 0.35F);
        for (int level = 0; level < LEVELS; level++) {
            float[] r = row(table.values, level);
            assertUnit(r, 0);
            assertUnit(r, 3);
            assertUnit(r, 6);
            assertOrthogonal(r, 0, 3);
            assertOrthogonal(r, 0, 6);
            assertOrthogonal(r, 3, 6);
            assertEquals(1.0F, determinant(r), 1.0E-4F, "level " + level + " mirrors");
        }
    }

    @Test
    void aQuarterTurnPitchLiftsTheUpAxisIntoZ() {
        CharacterRotationTable table = new CharacterRotationTable();
        table.update((float) (Math.PI / 2.0), 0.0F, 0.0F);
        float[] r = row(table.values, LEVELS - 1);
        assertEquals(0.0F, r[0] * 0.0F + r[1] * 1.0F + r[2] * 0.0F, TOLERANCE, "x should not change");
        assertEquals(0.0F, r[3] * 0.0F + r[4] * 1.0F + r[5] * 0.0F, 1.0E-4F, "y should rotate into z");
        assertEquals(1.0F, r[6] * 0.0F + r[7] * 1.0F + r[8] * 0.0F, 1.0E-4F, "y should rotate into z");
    }

    @Test
    void boneGeometryCoversEverySlotAndFlipsSides() {
        assertEquals(6, Bone.values().length, "the rig is a six-part humanoid");
        for (Bone bone : Bone.values()) {
            assertTrue(Float.isFinite(bone.pivotX) && Float.isFinite(bone.pivotY) && Float.isFinite(bone.pivotZ));
            assertSame(bone, bone.opposite().opposite(), bone + " does not flip back");
        }
        assertTrue(Bone.RIGHT_ARM.arm() && Bone.LEFT_ARM.arm() && !Bone.RIGHT_ARM.leg());
        assertSame(Bone.LEFT_ARM, Bone.RIGHT_ARM.opposite());
        assertSame(Bone.RIGHT_LEG, Bone.LEFT_LEG.opposite());
        assertSame(Bone.HEAD, Bone.HEAD.opposite());
    }

    private static float[] row(float[] values, int level) {
        float[] r = new float[9];
        System.arraycopy(values, level * 9, r, 0, 9);
        return r;
    }

    private static void assertMatrix(float[] values, int level, float... expected) {
        float[] actual = row(values, level);
        for (int i = 0; i < 9; i++) {
            assertEquals(expected[i], actual[i], TOLERANCE, "element " + i + " of level " + level);
        }
    }

    private static void assertUnit(float[] r, int offset) {
        float length = r[offset] * r[offset] + r[offset + 1] * r[offset + 1] + r[offset + 2] * r[offset + 2];
        assertEquals(1.0F, length, 1.0E-4F, "row at " + offset + " is not unit length");
    }

    private static void assertOrthogonal(float[] r, int a, int b) {
        float dot = r[a] * r[b] + r[a + 1] * r[b + 1] + r[a + 2] * r[b + 2];
        assertEquals(0.0F, dot, 1.0E-4F, "rows at " + a + " and " + b + " are not orthogonal");
    }

    private static float determinant(float[] r) {
        return r[0] * (r[4] * r[8] - r[5] * r[7])
            - r[1] * (r[3] * r[8] - r[5] * r[6])
            + r[2] * (r[3] * r[7] - r[4] * r[6]);
    }
}
