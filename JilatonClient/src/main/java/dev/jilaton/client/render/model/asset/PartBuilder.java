package dev.jilaton.client.render.model.asset;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Accumulates the faces of one convex solid, then emits a flat-shaded {@link MeshPart}.
 * <p>
 * Faces are recorded as polygons in any winding. {@link #finish()} computes the
 * centroid of the part's distinct vertices, takes each polygon's normal with Newell's
 * method and orients it away from that centroid, so both the stored normal and the
 * triangle winding point outward. For a convex solid the vertex centroid always lies
 * strictly inside, which is what makes that orientation unambiguous.
 */
final class PartBuilder {

    private final String owner;
    private final String material;
    private final List<float[][]> faces = new ArrayList<>();
    private final List<float[]> vertices = new ArrayList<>();
    private final Set<String> vertexKeys = new HashSet<>();

    PartBuilder(String owner, String material) {
        this.owner = owner;
        this.material = MaterialTags.require(material);
    }

    String material() {
        return material;
    }

    PartBuilder face(float[]... points) {
        if (points.length < 3) {
            throw new IllegalArgumentException(owner + "/" + material + ": a face needs at least 3 points");
        }
        float[][] copy = new float[points.length][];
        for (int i = 0; i < points.length; i++) {
            copy[i] = points[i].clone();
            // Track distinct vertices for the centroid; repeated corners do not count.
            String key = Float.floatToIntBits(copy[i][0]) + ":" + Float.floatToIntBits(copy[i][1])
                    + ":" + Float.floatToIntBits(copy[i][2]);
            if (vertexKeys.add(key)) {
                vertices.add(copy[i]);
            }
        }
        faces.add(copy);
        return this;
    }

    PartBuilder quad(float[] a, float[] b, float[] c, float[] d) {
        return face(a, b, c, d);
    }

    PartBuilder tri(float[] a, float[] b, float[] c) {
        return face(a, b, c);
    }

    /** Axis-aligned box from one corner to the opposite corner. */
    PartBuilder box(float x0, float y0, float z0, float x1, float y1, float z1) {
        return orientedBox(
                Geo.of((x0 + x1) * 0.5f, (y0 + y1) * 0.5f, (z0 + z1) * 0.5f),
                Geo.X, Geo.Y, Geo.Z,
                Math.abs(x1 - x0) * 0.5f, Math.abs(y1 - y0) * 0.5f, Math.abs(z1 - z0) * 0.5f);
    }

    /** Axis-aligned box from its centre and half extents. */
    PartBuilder boxAt(float cx, float cy, float cz, float hx, float hy, float hz) {
        return orientedBox(Geo.of(cx, cy, cz), Geo.X, Geo.Y, Geo.Z, hx, hy, hz);
    }

    /** Box with an arbitrary orthonormal frame; {@code ax/ay/az} must be unit and orthogonal. */
    PartBuilder orientedBox(float[] centre, float[] ax, float[] ay, float[] az,
                            float hx, float hy, float hz) {
        float[][] p = new float[8][];
        for (int i = 0; i < 8; i++) {
            float sx = (i & 4) == 0 ? -hx : hx;
            float sy = (i & 2) == 0 ? -hy : hy;
            float sz = (i & 1) == 0 ? -hz : hz;
            p[i] = Geo.add(centre, Geo.add(Geo.scale(ax, sx), Geo.add(Geo.scale(ay, sy), Geo.scale(az, sz))));
        }
        face(p[0], p[1], p[3], p[2]);
        face(p[4], p[5], p[7], p[6]);
        face(p[0], p[1], p[5], p[4]);
        face(p[2], p[3], p[7], p[6]);
        face(p[0], p[2], p[6], p[4]);
        face(p[1], p[3], p[7], p[5]);
        return this;
    }

    /** Extrudes a planar convex polygon along {@code offset}; both ends are capped. */
    PartBuilder prism(float[][] profile, float[] offset) {
        int n = profile.length;
        float[][] far = new float[n][];
        for (int i = 0; i < n; i++) {
            far[i] = Geo.add(profile[i], offset);
        }
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            quad(profile[i], profile[j], far[j], far[i]);
        }
        face(profile);
        face(far);
        return this;
    }

    /**
     * Connects equally sized rings in order. Each neighbouring pair becomes one band of
     * quads; the caps are optional so a curved run can be split into convex chunks.
     */
    PartBuilder loft(List<float[][]> rings, boolean capStart, boolean capEnd) {
        if (rings.size() < 2) {
            throw new IllegalArgumentException(owner + "/" + material + ": a loft needs at least 2 rings");
        }
        int sides = rings.get(0).length;
        for (int r = 0; r + 1 < rings.size(); r++) {
            float[][] a = rings.get(r);
            float[][] b = rings.get(r + 1);
            if (a.length != sides || b.length != sides) {
                throw new IllegalArgumentException(owner + "/" + material + ": all loft rings need the same point count");
            }
            for (int i = 0; i < sides; i++) {
                int j = (i + 1) % sides;
                quad(a[i], a[j], b[j], b[i]);
            }
        }
        if (capStart) {
            face(rings.get(0));
        }
        if (capEnd) {
            face(rings.get(rings.size() - 1));
        }
        return this;
    }

    /** Closes a ring with triangles to a single apex (a cone tip). No base cap. */
    PartBuilder apexFan(float[][] ring, float[] apex) {
        for (int i = 0; i < ring.length; i++) {
            tri(ring[i], ring[(i + 1) % ring.length], apex);
        }
        return this;
    }

    /** Faceted ellipsoid; {@code bands} is the number of latitude rows (>= 2). */
    PartBuilder sphere(float[] centre, float rx, float ry, float rz, int bands, int sides) {
        if (bands < 2 || sides < 3) {
            throw new IllegalArgumentException(owner + "/" + material + ": sphere needs bands >= 2 and sides >= 3");
        }
        List<float[][]> rings = new ArrayList<>();
        for (int i = 1; i < bands; i++) {
            double phi = -Math.PI / 2 + Math.PI * i / bands;
            float ringY = centre[1] + ry * (float) Math.sin(phi);
            float scale = (float) Math.cos(phi);
            rings.add(Geo.frameRing(Geo.of(centre[0], ringY, centre[2]),
                    Geo.X, Geo.Z, rx * scale, rz * scale, sides, 0f));
        }
        loft(rings, false, false);
        float[] bottom = Geo.of(centre[0], centre[1] - ry, centre[2]);
        float[] top = Geo.of(centre[0], centre[1] + ry, centre[2]);
        float[][] first = rings.get(0);
        float[][] last = rings.get(rings.size() - 1);
        for (int i = 0; i < sides; i++) {
            tri(bottom, first[i], first[(i + 1) % sides]);
            tri(last[i], last[(i + 1) % sides], top);
        }
        return this;
    }

    MeshPart finish() {
        if (faces.isEmpty()) {
            throw new IllegalStateException(owner + "/" + material + ": part has no faces");
        }
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (float[][] f : faces) {
            for (float[] p : f) {
                minX = Math.min(minX, p[0]);
                minY = Math.min(minY, p[1]);
                minZ = Math.min(minZ, p[2]);
                maxX = Math.max(maxX, p[0]);
                maxY = Math.max(maxY, p[1]);
                maxZ = Math.max(maxZ, p[2]);
            }
        }
        float[] centre = new float[3];
        for (float[] v : vertices) {
            centre[0] += v[0];
            centre[1] += v[1];
            centre[2] += v[2];
        }
        centre[0] /= vertices.size();
        centre[1] /= vertices.size();
        centre[2] /= vertices.size();
        float diagonal = (float) Math.sqrt(
                (maxX - minX) * (maxX - minX) + (maxY - minY) * (maxY - minY) + (maxZ - minZ) * (maxZ - minZ));

        List<float[]> positions = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        for (int f = 0; f < faces.size(); f++) {
            float[][] poly = faces.get(f);
            float[] normal = newell(poly);
            float[] offset = Geo.sub(polygonCentre(poly), centre);
            float outward = Geo.dot(normal, offset);
            if (Math.abs(outward) < 1e-5f * Math.max(diagonal, 1e-4f)) {
                throw new IllegalStateException(owner + "/" + material + " face " + f
                        + ": plane passes through the part centroid (dot=" + outward
                        + "); the shape is not convex enough to stand alone");
            }
            if (outward < 0f) {
                reverse(poly);
            }
            for (int i = 1; i < poly.length - 1; i++) {
                float[] a = poly[0];
                float[] b = poly[i];
                float[] c = poly[i + 1];
                float[] edge1 = Geo.sub(b, a);
                float[] edge2 = Geo.sub(c, a);
                float[] triNormal = Geo.cross(edge1, edge2);
                if (Geo.length(triNormal) < 1e-9f) {
                    throw new IllegalStateException(owner + "/" + material + " face " + f
                            + ": degenerate triangle at fan index " + i);
                }
                emit(positions, normals, indices, a, b, c, Geo.normalise(triNormal));
            }
        }

        float[] pos = new float[positions.size() * 3];
        float[] nrm = new float[normals.size() * 3];
        for (int i = 0; i < positions.size(); i++) {
            System.arraycopy(positions.get(i), 0, pos, i * 3, 3);
            System.arraycopy(normals.get(i), 0, nrm, i * 3, 3);
        }
        int[] idx = new int[indices.size()];
        for (int i = 0; i < idx.length; i++) {
            idx[i] = indices.get(i);
        }
        return new MeshPart(material, pos, nrm, idx);
    }

    private static void emit(List<float[]> positions, List<float[]> normals, List<Integer> indices,
                             float[] a, float[] b, float[] c, float[] normal) {
        int base = positions.size();
        positions.add(a);
        positions.add(b);
        positions.add(c);
        normals.add(normal);
        normals.add(normal);
        normals.add(normal);
        indices.add(base);
        indices.add(base + 1);
        indices.add(base + 2);
    }

    private static float[] newell(float[][] poly) {
        float nx = 0f, ny = 0f, nz = 0f;
        for (int i = 0; i < poly.length; i++) {
            float[] a = poly[i];
            float[] b = poly[(i + 1) % poly.length];
            nx += (a[1] - b[1]) * (a[2] + b[2]);
            ny += (a[2] - b[2]) * (a[0] + b[0]);
            nz += (a[0] - b[0]) * (a[1] + b[1]);
        }
        return Geo.normalise(Geo.of(nx, ny, nz));
    }

    private static float[] polygonCentre(float[][] poly) {
        float x = 0f, y = 0f, z = 0f;
        for (float[] p : poly) {
            x += p[0];
            y += p[1];
            z += p[2];
        }
        return Geo.of(x / poly.length, y / poly.length, z / poly.length);
    }

    private static void reverse(float[][] poly) {
        for (int i = 0, j = poly.length - 1; i < j; i++, j--) {
            float[] tmp = poly[i];
            poly[i] = poly[j];
            poly[j] = tmp;
        }
    }
}
