package dev.jilaton.client.render.model.asset;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of the hand-authored model assets.
 * <p>
 * Every vertex, normal and triangle here is produced in code by the builders in this
 * package. Nothing is extracted, converted, resampled or otherwise derived from a
 * third-party model file, jar or reference asset: the knives are original geometry in
 * the general shape language of counter-strike style knives. Part materials are plain
 * colour tags ({@code blade}, {@code edge}, {@code handle}, {@code rivet}, {@code accent});
 * the renderer owns the tag-to-colour mapping. Player characters live in
 * {@code render.character} (the Anubis port), not here.
 */
public final class ModelAssets {

    private static final Map<String, MeshData> KNIVES = new LinkedHashMap<>();

    static {
        register(KNIVES, Knives.bayonet());
        register(KNIVES, Knives.karambit());
        register(KNIVES, Knives.butterfly());
        register(KNIVES, Knives.tanto());
    }

    private ModelAssets() {
    }

    private static void register(Map<String, MeshData> target, MeshData model) {
        if (target.put(model.id(), model) != null) {
            throw new IllegalStateException("duplicate model id " + model.id());
        }
    }

    /** Ids of every knife, in registration order. */
    public static List<String> knifeIds() {
        return List.copyOf(KNIVES.keySet());
    }

    /** The knife with this id; throws on an unknown id. */
    public static MeshData knife(String id) {
        MeshData model = KNIVES.get(id);
        if (model == null) {
            throw new IllegalArgumentException("unknown knife id: " + id + " (known: " + knifeIds() + ")");
        }
        return model;
    }

    /** Any registered model; throws on an unknown id. */
    public static MeshData get(String id) {
        MeshData model = KNIVES.get(id);
        if (model == null) {
            throw new IllegalArgumentException("unknown model id: " + id);
        }
        return model;
    }
}
