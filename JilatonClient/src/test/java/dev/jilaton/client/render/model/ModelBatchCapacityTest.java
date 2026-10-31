package dev.jilaton.client.render.model;

import dev.jilaton.client.render.EffectBatch;
import dev.jilaton.client.render.fx.FxFrame;
import dev.jilaton.client.render.fx.VisualQuality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelBatchCapacityTest {
    @Test
    void aFullCrowdOfWorstCaseWingsStillFits() {
        Mesh feather = Meshes.strip(WingMotion.segments("Feather", 3), WingMotion.halfWidth("Feather"),
                WingMotion.tipWidth("Feather"), WingMotion.fold("Feather"), WingMotion.bend("Feather"));
        Material material = new Material(0x101010, 0xF0F0F0, 0x8844FF, 0.2);
        FxFrame frame = new FxFrame(null, new EffectBatch(), 0, 0, 0,
                new float[]{1, 0, 0}, new float[]{0, 1, 0}, new float[]{0, 0, 1},
                0, 0.016, 0, 0xFFFFFF, VisualQuality.CINEMATIC, false, 1, false);

        int perPlayer = 0;
        for (int layer = 0; layer < WingMotion.layers("Feather"); layer++) {
            perPlayer += WingMotion.feathers("Feather", layer, 3) * 2;
        }
        int crowd = perPlayer * 16;
        assertEquals(960, crowd, "sixteen ULTRA-Feather players are the worst supported case");

        ModelBatch batch = new ModelBatch();
        for (int i = 0; i < crowd; i++) {
            batch.add(frame, feather, material, Transform.translation(0, 1, 0), 1, 1, false);
        }
        assertEquals(crowd, batch.size(), "the model batch dropped wings past its cap");
    }
}
