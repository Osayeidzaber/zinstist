package dev.jilaton.client.render.model.asset;

import java.util.ArrayList;
import java.util.List;

/** Collects a model's parts and turns them into a {@link MeshData} with measured bounds. */
final class ModelBuilder {

    private final String id;
    private final List<PartBuilder> parts = new ArrayList<>();

    ModelBuilder(String id) {
        this.id = id;
    }

    PartBuilder part(String material) {
        PartBuilder part = new PartBuilder(id, material);
        parts.add(part);
        return part;
    }

    MeshData build() {
        List<MeshPart> built = new ArrayList<>(parts.size());
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (PartBuilder part : parts) {
            MeshPart mesh = part.finish();
            built.add(mesh);
            float[] positions = mesh.positions();
            for (int i = 0; i < positions.length; i += 3) {
                minX = Math.min(minX, positions[i]);
                minY = Math.min(minY, positions[i + 1]);
                minZ = Math.min(minZ, positions[i + 2]);
                maxX = Math.max(maxX, positions[i]);
                maxY = Math.max(maxY, positions[i + 1]);
                maxZ = Math.max(maxZ, positions[i + 2]);
            }
        }
        if (built.isEmpty()) {
            throw new IllegalStateException(id + ": model has no parts");
        }
        return new MeshData(id, built, minX, minY, minZ, maxX, maxY, maxZ);
    }
}
