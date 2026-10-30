package dev.jilaton.client.render.model.asset;

import java.util.List;

/**
 * The colour tags the renderer understands. A model may only use these strings as its
 * part materials; the renderer owns the mapping from tag to colour.
 */
final class MaterialTags {

    static final List<String> KNOWN = List.of(
            "blade", "edge", "handle", "rivet", "wood",
            "cloth", "cloth_light", "skin", "eye", "accent", "bat");

    private MaterialTags() {
    }

    static String require(String tag) {
        if (tag == null || !KNOWN.contains(tag)) {
            throw new IllegalArgumentException("unknown material tag: " + tag + " (known: " + KNOWN + ")");
        }
        return tag;
    }
}
