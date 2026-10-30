package dev.jilaton.client.render;

import net.minecraft.resources.Identifier;

/** Pipeline locations must be valid namespaced identifiers; a raw "jilaton:" string gets a minecraft: prefix. */
final class OverlayPipelineIds {
    private OverlayPipelineIds() { }

    static Identifier of(boolean line, boolean throughWalls) {
        return Identifier.fromNamespaceAndPath("jilaton",
                "pipeline/" + (line ? "outline" : "surface") + (throughWalls ? "_overlay" : "_depth"));
    }

    static Identifier sky() {
        return Identifier.fromNamespaceAndPath("jilaton", "pipeline/sky_screen");
    }
}
