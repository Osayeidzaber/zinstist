package dev.jilaton.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverlayPipelineIdTest {
    @Test
    void pipelineLocationsAreValidJilatonIdentifiers() {
        // Regression: "jilaton:jilaton/..." was passed as a path and crashed the first world frame.
        for (boolean line : new boolean[]{false, true}) for (boolean walls : new boolean[]{false, true})
            assertEquals("jilaton", OverlayPipelineIds.of(line, walls).getNamespace());
        assertEquals("jilaton:pipeline/surface_depth", OverlayPipelineIds.of(false, false).toString());
    }
}
