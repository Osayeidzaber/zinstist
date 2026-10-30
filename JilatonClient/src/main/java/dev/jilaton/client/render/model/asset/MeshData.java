package dev.jilaton.client.render.model.asset;

import java.util.List;
import java.util.Objects;

/**
 * A complete model: its parts plus the axis-aligned bounds of all of them.
 * <p>
 * Bounds are in model space (blocks) and are measured from the emitted vertices, not
 * declared by hand.
 */
public record MeshData(String id, List<MeshPart> parts,
                       float minX, float minY, float minZ,
                       float maxX, float maxY, float maxZ) {

    public MeshData {
        Objects.requireNonNull(id, "id");
        parts = List.copyOf(parts);
    }

    public int partCount() {
        return parts.size();
    }

    public int triangleCount() {
        int total = 0;
        for (MeshPart part : parts) {
            total += part.triangleCount();
        }
        return total;
    }

    public float width() {
        return maxX - minX;
    }

    public float height() {
        return maxY - minY;
    }

    public float depth() {
        return maxZ - minZ;
    }
}
