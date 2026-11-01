package dev.jilaton.client.render.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlassCubeTest {
    @Test
    void rotatedCubeKeepsEveryEdgeTheSameLength() {
        double[][] c = FxShapes.cubeCorners(0.5, 0.7, 1.9, -0.4);
        for (int[] e : FxShapes.CUBE_EDGES) {
            double dx = c[e[0]][0] - c[e[1]][0], dy = c[e[0]][1] - c[e[1]][1], dz = c[e[0]][2] - c[e[1]][2];
            assertEquals(1.0, Math.sqrt(dx * dx + dy * dy + dz * dz), 1e-9);
        }
    }

    @Test
    void facesUseFourDistinctCornersThatAreAllEdges() {
        for (int[] q : FxShapes.CUBE_FACES)
            for (int i = 0; i < 4; i++) {
                int a = q[i], b = q[(i + 1) % 4];
                assertEquals(1, Integer.bitCount(a ^ b), "face side " + a + "-" + b + " is not a cube edge");
            }
    }
}
