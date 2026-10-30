package dev.jilaton.client.render.model.asset;

import java.util.Objects;

/**
 * One material-homogeneous, closed chunk of model geometry.
 * <p>
 * {@code positions} and {@code normals} are flat {@code x, y, z} arrays in model space
 * (blocks); {@code indices} addresses those vertices three at a time, one triangle per
 * triple. Normals are per-vertex and therefore flat-shaded: every corner of a triangle
 * repeats that triangle's face normal.
 */
public record MeshPart(String material, float[] positions, float[] normals, int[] indices) {

    public MeshPart {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(positions, "positions");
        Objects.requireNonNull(normals, "normals");
        Objects.requireNonNull(indices, "indices");
        positions = positions.clone();
        normals = normals.clone();
        indices = indices.clone();
    }

    public int vertexCount() {
        return positions.length / 3;
    }

    public int triangleCount() {
        return indices.length / 3;
    }
}
