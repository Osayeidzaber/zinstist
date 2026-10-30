package dev.jilaton.client.render.fx;

import dev.jilaton.client.render.EffectBatch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One fixed-capacity particle and decal pool shared by every module. Storage is struct-of-arrays, so
 * spawning and simulating never allocate. When the active quality budget is reached the oldest
 * particle is recycled, so bursts can never grow memory or frame cost without bound.
 */
public final class FxParticles {
    public static final int CAPACITY = 4096;
    public static final int DECAL_CAPACITY = 192;

    public enum Shape { DOT, SPARK, STAR, RING, SMOKE, CRYSTAL, DROPLET }

    private static final Shape[] SHAPES = Shape.values();
    private static final FxParticles INSTANCE = new FxParticles();

    private final double[] x = new double[CAPACITY], y = new double[CAPACITY], z = new double[CAPACITY];
    private final double[] vx = new double[CAPACITY], vy = new double[CAPACITY], vz = new double[CAPACITY];
    private final double[] gravity = new double[CAPACITY], drag = new double[CAPACITY], floor = new double[CAPACITY];
    private final double[] age = new double[CAPACITY], life = new double[CAPACITY];
    private final double[] size0 = new double[CAPACITY], size1 = new double[CAPACITY], alpha = new double[CAPACITY];
    private final double[] rot = new double[CAPACITY], spin = new double[CAPACITY];
    private final int[] color = new int[CAPACITY], color1 = new int[CAPACITY], owner = new int[CAPACITY];
    private final byte[] shape = new byte[CAPACITY];
    private final boolean[] glow = new boolean[CAPACITY], walls = new boolean[CAPACITY];
    private int count;

    private final double[] dx = new double[DECAL_CAPACITY], dy = new double[DECAL_CAPACITY], dz = new double[DECAL_CAPACITY];
    private final double[] dRadius = new double[DECAL_CAPACITY], dAge = new double[DECAL_CAPACITY], dLife = new double[DECAL_CAPACITY];
    private final double[] dAlpha = new double[DECAL_CAPACITY];
    private final long[] dSeed = new long[DECAL_CAPACITY];
    private final int[] dColor = new int[DECAL_CAPACITY], dOwner = new int[DECAL_CAPACITY];
    private int decals;
    private long seedCounter = 0x51F3L;
    private int budget = VisualQuality.BALANCED.particleBudget();

    public static FxParticles get() { return INSTANCE; }

    public static int ownerId(String moduleId) { return moduleId.hashCode(); }

    public int count() { return count; }
    public int decalCount() { return decals; }

    public void setBudget(int budget) { this.budget = Math.max(16, Math.min(CAPACITY, budget)); }

    /** Fluent spawn description reused by callers to avoid allocation; call {@link #spawn(Spec)} to commit. */
    public static final class Spec {
        public double x, y, z, vx, vy, vz, gravity, drag = 0.9, floor = Double.NaN, life = 1, size0 = 0.08, size1 = 0;
        public double alpha = 1, rot, spin;
        public int color = 0xFFFFFF, color1 = -1, owner;
        public Shape shape = Shape.DOT;
        public boolean glow, walls;

        public Spec reset() {
            x = y = z = vx = vy = vz = gravity = rot = spin = 0;
            drag = 0.9; floor = Double.NaN; life = 1; size0 = 0.08; size1 = 0; alpha = 1;
            color = 0xFFFFFF; color1 = -1; owner = 0; shape = Shape.DOT; glow = false; walls = false;
            return this;
        }
    }

    public void spawn(Spec s) {
        int i;
        if (count < budget) i = count++;
        else i = oldest();
        x[i] = s.x; y[i] = s.y; z[i] = s.z; vx[i] = s.vx; vy[i] = s.vy; vz[i] = s.vz;
        gravity[i] = s.gravity; drag[i] = s.drag; floor[i] = s.floor; age[i] = 0; life[i] = Math.max(0.05, s.life);
        size0[i] = s.size0; size1[i] = s.size1; alpha[i] = s.alpha; rot[i] = s.rot; spin[i] = s.spin;
        color[i] = s.color; color1[i] = s.color1 < 0 ? s.color : s.color1; owner[i] = s.owner;
        shape[i] = (byte) s.shape.ordinal(); glow[i] = s.glow; walls[i] = s.walls;
    }

    private final Spec addSpec = new Spec();

    /**
     * One-call spawn for modules that describe a particle inline. {@code dampingPerSecond} is a rate (0 = none,
     * ~1-2 = gentle), converted to this simulation's per-tick drag factor; values here are never a raw factor, so a
     * caller can never make particles accelerate.
     */
    public void add(double x, double y, double z, double vx, double vy, double vz, double gravity,
                    double dampingPerSecond, double floor, double life, double size0, double size1, int color,
                    int color1, double alpha, Shape shape, boolean glow, boolean walls, double spin, int owner) {
        Spec s = addSpec.reset();
        s.x = x; s.y = y; s.z = z; s.vx = vx; s.vy = vy; s.vz = vz; s.gravity = gravity;
        s.drag = Math.exp(-Math.max(0, dampingPerSecond) / 20.0);
        s.floor = floor; s.life = life; s.size0 = size0; s.size1 = size1; s.color = color; s.color1 = color1;
        s.alpha = alpha; s.shape = shape; s.glow = glow; s.walls = walls; s.spin = spin; s.owner = owner;
        spawn(s);
    }

    private int oldest() {
        int best = 0;
        double bestRatio = -1;
        for (int i = 0; i < count; i++) {
            double ratio = age[i] / life[i];
            if (ratio > bestRatio) { bestRatio = ratio; best = i; }
        }
        return best;
    }

    public void decal(double x, double y, double z, double radius, double life, int rgb, double alpha, int owner) {
        int i;
        if (decals < DECAL_CAPACITY) i = decals++;
        else {
            i = 0;
            for (int k = 1; k < decals; k++) if (dAge[k] / dLife[k] > dAge[i] / dLife[i]) i = k;
        }
        dx[i] = x; dy[i] = y; dz[i] = z; dRadius[i] = radius; dAge[i] = 0; dLife[i] = Math.max(0.1, life);
        dAlpha[i] = alpha; dColor[i] = rgb; dOwner[i] = owner; dSeed[i] = seedCounter++ * 0x2545F4914F6CDD1DL;
    }

    public void clear() { count = 0; decals = 0; }

    public void clearOwner(int ownerId) {
        for (int i = count - 1; i >= 0; i--) if (owner[i] == ownerId) removeParticle(i);
        for (int i = decals - 1; i >= 0; i--) if (dOwner[i] == ownerId) removeDecal(i);
    }

    /** Advances simulation by the frame's dt and appends geometry. Called once per frame by the renderer. */
    public void step(FxFrame f) {
        setBudget(f.quality.particleBudget());
        double dt = f.dt;
        for (int i = count - 1; i >= 0; i--) {
            age[i] += dt;
            if (age[i] >= life[i]) { removeParticle(i); continue; }
            double damping = Math.pow(drag[i], dt * 20);
            vx[i] *= damping; vz[i] *= damping; vy[i] = vy[i] * damping - gravity[i] * dt;
            x[i] += vx[i] * dt; y[i] += vy[i] * dt; z[i] += vz[i] * dt;
            rot[i] += spin[i] * dt;
            if (!Double.isNaN(floor[i]) && y[i] <= floor[i]) {
                y[i] = floor[i];
                if (shape[i] == Shape.DROPLET.ordinal()) {
                    // Ink droplets become small splats where they land.
                    decal(x[i], floor[i] + 0.012, z[i], size0[i] * 2.4, Math.max(0.6, life[i] - age[i] + 0.8), color[i], alpha[i], owner[i]);
                    removeParticle(i);
                    continue;
                }
                vy[i] = Math.abs(vy[i]) * 0.25; vx[i] *= 0.6; vz[i] *= 0.6;
            }
        }
        for (int i = decals - 1; i >= 0; i--) {
            dAge[i] += dt;
            if (dAge[i] >= dLife[i]) removeDecal(i);
        }
        draw(f);
    }

    private void draw(FxFrame f) {
        double maxDistSqr = 96 * 96;
        for (int i = 0; i < decals; i++) {
            double t = dAge[i] / dLife[i];
            double spread = 1 - Math.pow(1 - Math.min(1, dAge[i] / 0.18), 3);
            double fade = t < 0.7 ? 1 : 1 - (t - 0.7) / 0.3;
            if (f.distanceSqr(dx[i], dy[i], dz[i]) > maxDistSqr) continue;
            FxShapes.splat(f, dx[i], dy[i], dz[i], dRadius[i], dSeed[i], spread, dColor[i], dAlpha[i] * fade, false);
        }
        for (int i = 0; i < count; i++) {
            if (f.distanceSqr(x[i], y[i], z[i]) > maxDistSqr) continue;
            double t = age[i] / life[i];
            double size = size0[i] + (size1[i] - size0[i]) * t;
            double a = alpha[i] * fadeCurve(t);
            int rgb = color[i] == color1[i] ? color[i] : EffectBatch.mixRgb(color[i], color1[i], t);
            Shape s = SHAPES[shape[i]];
            switch (s) {
                case DOT -> FxShapes.glowDot(f, x[i], y[i], z[i], size, rgb, a, glow[i], walls[i]);
                case SMOKE -> FxShapes.softDot(f, x[i], y[i], z[i], size, rgb, a * 0.5, walls[i]);
                case SPARK, DROPLET -> {
                    double speed = Math.sqrt(vx[i] * vx[i] + vy[i] * vy[i] + vz[i] * vz[i]);
                    double length = Math.min(0.7, size * 2 + speed * 0.045);
                    if (glow[i]) FxShapes.softDot(f, x[i], y[i], z[i], size * 1.6, rgb, a * 0.25, walls[i]);
                    FxShapes.spark(f, x[i], y[i], z[i], vx[i], vy[i], vz[i], length, size * 0.5, rgb, a, walls[i]);
                }
                case STAR -> {
                    if (glow[i]) FxShapes.softDot(f, x[i], y[i], z[i], size * 2.2, rgb, a * 0.25, walls[i]);
                    FxShapes.star(f, x[i], y[i], z[i], size, rot[i], 4, rgb, a, walls[i]);
                }
                case RING -> FxShapes.billboardRing(f, x[i], y[i], z[i], size, Math.max(0.01, size * 0.12), rgb, a, walls[i]);
                case CRYSTAL -> {
                    if (glow[i]) FxShapes.softDot(f, x[i], y[i], z[i], size * 1.9, rgb, a * 0.28, walls[i]);
                    FxShapes.crystal(f, x[i], y[i], z[i], size * 0.6, size, rot[i], rgb, a, walls[i]);
                }
            }
        }
    }

    private static double fadeCurve(double t) {
        double in = Math.min(1, t / 0.08);
        double out = t < 0.55 ? 1 : 1 - (t - 0.55) / 0.45;
        return in * out * out;
    }

    private void removeParticle(int i) {
        int last = --count;
        if (i == last) return;
        x[i] = x[last]; y[i] = y[last]; z[i] = z[last]; vx[i] = vx[last]; vy[i] = vy[last]; vz[i] = vz[last];
        gravity[i] = gravity[last]; drag[i] = drag[last]; floor[i] = floor[last]; age[i] = age[last]; life[i] = life[last];
        size0[i] = size0[last]; size1[i] = size1[last]; alpha[i] = alpha[last]; rot[i] = rot[last]; spin[i] = spin[last];
        color[i] = color[last]; color1[i] = color1[last]; owner[i] = owner[last]; shape[i] = shape[last];
        glow[i] = glow[last]; walls[i] = walls[last];
    }

    private void removeDecal(int i) {
        int last = --decals;
        if (i == last) return;
        dx[i] = dx[last]; dy[i] = dy[last]; dz[i] = dz[last]; dRadius[i] = dRadius[last]; dAge[i] = dAge[last];
        dLife[i] = dLife[last]; dAlpha[i] = dAlpha[last]; dSeed[i] = dSeed[last]; dColor[i] = dColor[last]; dOwner[i] = dOwner[last];
    }

    /** Top of the first solid surface at or below {@code y}, searching {@code depth} blocks; NaN if none. */
    public static double groundY(Level level, double x, double y, double z, int depth) {
        if (level == null) return Double.NaN;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z), top = (int) Math.floor(y);
        for (int by = top; by >= top - depth; by--) {
            pos.set(bx, by, bz);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            var shape = state.getCollisionShape(level, pos);
            if (shape.isEmpty()) continue;
            double surface = by + shape.max(Direction.Axis.Y);
            if (surface <= y + 0.01) return surface;
        }
        return Double.NaN;
    }
}
