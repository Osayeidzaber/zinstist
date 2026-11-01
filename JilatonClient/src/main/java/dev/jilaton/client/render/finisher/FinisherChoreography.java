package dev.jilaton.client.render.finisher;

import java.util.List;

/**
 * Keyframed finishing moves for the 3D character. Pure math so the timing is testable: a move is a list of
 * keys, each a full body pose (vanilla model angles, x/y/z per bone) plus how far the character has stepped
 * toward the defeated entity, how high it is off the ground and an extra spin. Neighbouring keys are blended
 * with smootherstep, so every limb eases in and out and nothing snaps.
 */
public final class FinisherChoreography {
    /** Bone order matches {@code kit.Bone}: head, body, right arm, left arm, right leg, left leg. */
    public static final int BONES = 6;
    public static final int FORWARD = BONES * 3;
    public static final int RISE = FORWARD + 1;
    public static final int SPIN = FORWARD + 2;
    static final int WIDTH = SPIN + 1;

    public static final float APPEAR = 0.32F;
    public static final float FADE = 0.5F;

    public enum Variant {
        GROUND_SLAM("Ground slam", 1.02F, 2.25F),
        RISING_UPPERCUT("Rising uppercut", 0.86F, 2.1F),
        SPIN_KICK("Spin kick", 0.94F, 2.15F),
        /** Tung Tung Tung Sahur: three bat knocks on the body, then a jump and a heavy final slam. */
        TUNG_BONK("Tung tung bonk", 1.72F, 2.75F, 0.62F, 0.98F, 1.34F);

        public final String label;
        /** Seconds from start to the hit, where particles, sound and shake fire. */
        public final float impact;
        public final float length;
        /** Lighter hits before the main impact (seconds); each gets a knock sound and a few sparks. */
        public final float[] knocks;

        Variant(String label, float impact, float length, float... knocks) {
            this.label = label;
            this.impact = impact;
            this.length = length;
            this.knocks = knocks;
        }

        /** The character this move is always performed by, or null to use the selected one. */
        public String character() {
            return this == TUNG_BONK ? "Tung Tung Tung Sahur" : null;
        }

        public static Variant byLabel(String label) {
            for (Variant v : values()) if (v.label.equals(label)) return v;
            return GROUND_SLAM;
        }
    }

    public static final List<String> LABELS = List.of("Ground slam", "Rising uppercut", "Spin kick", "Tung tung bonk", "Random");

    private static final float[] REST = pose(
            0, 0, 0,
            0, 0, 0,
            0, 0, 0.1F,
            0, 0, -0.1F,
            0, 0, 0,
            0, 0, 0,
            0, 0, 0);

    private static final float[][] SLAM_KEYS = {
            key(0F, REST),
            // Crouch: weight down, fists drawn back.
            key(0.36F, pose(0.25F, 0, 0, 0.32F, 0, 0, 0.55F, 0, 0.35F, 0.55F, 0, -0.35F, -0.45F, 0, 0.05F, 0.3F, 0, -0.05F,
                    0.05F, -0.1F, 0)),
            // Apex: leap with both fists locked overhead.
            key(0.74F, pose(-0.35F, 0, 0, -0.3F, 0, 0, -2.95F, 0, -0.18F, -2.95F, 0, 0.18F, 0.45F, 0, 0.08F, -0.35F, 0, -0.08F,
                    0.35F, 0.85F, 0)),
            // Impact: hammer down into the ground beside the body.
            key(1.02F, pose(0.55F, 0, 0, 0.62F, 0, 0, -0.7F, 0, -0.12F, -0.7F, 0, 0.12F, -0.85F, 0, 0.12F, 0.6F, 0, -0.1F,
                    0.62F, -0.06F, 0)),
            key(1.5F, pose(0.4F, 0, 0, 0.5F, 0, 0, -0.55F, 0, -0.1F, -0.55F, 0, 0.1F, -0.75F, 0, 0.1F, 0.5F, 0, -0.08F,
                    0.62F, -0.05F, 0)),
            key(2.25F, REST, 0.45F),
    };

    private static final float[][] UPPERCUT_KEYS = {
            key(0F, REST),
            // Coil: torso twisted away, right fist low.
            key(0.4F, pose(0.15F, 0.35F, 0, 0.28F, 0.45F, 0, 0.75F, 0, 0.25F, -1.05F, 0, -0.4F, -0.4F, 0, 0.05F, 0.35F, 0, 0,
                    0.1F, -0.08F, 0)),
            // Launch: the fist drives straight up through the target.
            key(0.86F, pose(-0.45F, -0.35F, 0, -0.32F, -0.4F, 0, -2.9F, 0.2F, 0.12F, 0.6F, 0, -0.45F, 0.35F, 0, 0, -0.65F, 0, 0,
                    0.55F, 0.55F, 0)),
            key(1.45F, pose(-0.25F, -0.2F, 0, -0.15F, -0.2F, 0, -2.6F, 0.1F, 0.1F, 0.4F, 0, -0.35F, 0.15F, 0, 0, -0.3F, 0, 0,
                    0.55F, 0.05F, 0)),
            key(2.1F, REST, 0.55F),
    };

    private static final float[] BONK_UP = pose(
            -0.25F, 0, 0, -0.18F, 0, 0, -2.7F, 0, -0.15F, -0.4F, 0, -0.2F, -0.2F, 0, 0, 0.25F, 0, 0,
            0.4F, 0, 0);
    private static final float[] BONK_DOWN = pose(
            0.35F, 0, 0, 0.4F, 0, 0, -0.55F, 0, -0.08F, -0.3F, 0, -0.2F, -0.45F, 0, 0, 0.4F, 0, 0,
            0.52F, -0.03F, 0);

    private static final float[][] BONK_KEYS = {
            key(0F, REST),
            key(0.3F, BONK_UP),
            key(0.62F, BONK_DOWN),
            key(0.8F, BONK_UP),
            key(0.98F, BONK_DOWN),
            key(1.16F, BONK_UP),
            key(1.34F, BONK_DOWN),
            // Jump with the bat high, then the heavy slam.
            key(1.5F, pose(-0.35F, 0, 0, -0.3F, 0, 0, -2.75F, 0, -0.15F, -2.2F, 0, 0.15F, 0.4F, 0, 0, -0.3F, 0, 0,
                    0.55F, 0.35F, 0)),
            key(1.72F, pose(0.55F, 0, 0, 0.62F, 0, 0, -0.6F, 0, -0.1F, -0.6F, 0, 0.1F, -0.8F, 0, 0.1F, 0.55F, 0, -0.1F,
                    0.62F, 0F, 0)),
            key(2.2F, pose(0.4F, 0, 0, 0.45F, 0, 0, -0.5F, 0, -0.1F, -0.5F, 0, 0.1F, -0.7F, 0, 0.1F, 0.45F, 0, -0.08F,
                    0.62F, 0F, 0)),
            key(2.75F, REST, 0.5F),
    };

    private static final float TURN = (float) (Math.PI * 2);

    private static final float[][] SPIN_KEYS = {
            key(0F, REST),
            key(0.34F, pose(0.1F, 0, 0, 0.12F, 0, 0, -0.35F, 0, 0.6F, -0.35F, 0, -0.6F, -0.3F, 0, 0, 0.25F, 0, 0,
                    0.05F, -0.05F, 0)),
            // Wind: half a turn with arms flung out for balance.
            key(0.62F, pose(0, 0, 0, 0, 0, 0, 0, 0, 1.25F, 0, 0, -1.25F, -0.6F, 0, 0.35F, 0.2F, 0, 0,
                    0.25F, 0.3F, TURN * 0.5F)),
            // Strike: full turn, right leg extended at head height.
            key(0.94F, pose(0, 0, 0, -0.15F, 0, 0, 0.3F, 0, 1.0F, 0.3F, 0, -1.0F, -1.55F, 0, 0.55F, 0.15F, 0, 0,
                    0.55F, 0.22F, TURN)),
            key(1.45F, pose(0, 0, 0, -0.05F, 0, 0, 0, 0, 0.5F, 0, 0, -0.5F, -0.4F, 0, 0.15F, 0.1F, 0, 0,
                    0.55F, 0F, TURN)),
            key(2.15F, REST, 0.5F, TURN),
    };

    private FinisherChoreography() { }

    static float[][] keys(Variant variant) {
        return switch (variant) {
            case GROUND_SLAM -> SLAM_KEYS;
            case RISING_UPPERCUT -> UPPERCUT_KEYS;
            case SPIN_KICK -> SPIN_KEYS;
            case TUNG_BONK -> BONK_KEYS;
        };
    }

    /** Writes the blended pose at {@code t} seconds into {@code out} (length {@link #WIDTH} or more). */
    public static float[] sample(Variant variant, float t, float[] out) {
        float[][] keys = keys(variant);
        if (t <= keys[0][0]) {
            System.arraycopy(keys[0], 1, out, 0, WIDTH);
            return out;
        }
        for (int i = 1; i < keys.length; i++) {
            float[] b = keys[i];
            if (t <= b[0]) {
                float[] a = keys[i - 1];
                float k = smoother((t - a[0]) / (b[0] - a[0]));
                for (int j = 0; j < WIDTH; j++) out[j] = a[j + 1] + (b[j + 1] - a[j + 1]) * k;
                return out;
            }
        }
        System.arraycopy(keys[keys.length - 1], 1, out, 0, WIDTH);
        return out;
    }

    /** Opacity: materialises over {@link #APPEAR}, holds, then dissolves over the last {@link #FADE} seconds. */
    public static float alpha(Variant variant, float t) {
        if (t <= 0 || t >= variant.length) return 0F;
        return Math.min(smoother(t / APPEAR), smoother((variant.length - t) / FADE));
    }

    /** Size: grows in with a small overshoot so the arrival reads as a pop rather than a fade. */
    public static float scale(Variant variant, float t) {
        float in = clamp01(t / APPEAR);
        float pop = 1F - (1F - in) * (1F - in) * (1F - in);
        float overshoot = (float) Math.sin(in * Math.PI) * 0.07F;
        float out = clamp01((variant.length - t) / FADE);
        return (0.82F + 0.18F * pop + overshoot) * (0.92F + 0.08F * out);
    }

    /** Camera kick strength at {@code t}: a sharp spike at the hit that decays within a third of a second. */
    public static float shake(Variant variant, float t) {
        float since = t - variant.impact;
        if (since < 0 || since > 0.35F) return 0F;
        float k = 1F - since / 0.35F;
        return k * k;
    }

    /** Seconds a crowd member takes to walk in from {@link #CROWD_FROM} to {@link #CROWD_TO} blocks. */
    public static final float CROWD_WALK = 1.1F;
    public static final float CROWD_FROM = 5.5F;
    public static final float CROWD_TO = 2.4F;

    /**
     * Pose for a crowd member at {@code t}: walks in with a swinging stride, slows to a stop, then cheers with
     * both arms up and small hops. {@code phase} offsets each member so they are not in lockstep. Writes the
     * distance from the body into {@link #FORWARD} (blocks) and the hop into {@link #RISE}.
     */
    public static float[] crowd(float t, float phase, float[] out) {
        java.util.Arrays.fill(out, 0, WIDTH, 0F);
        float walk = clamp01(t / CROWD_WALK);
        float eased = 1F - (1F - walk) * (1F - walk);
        out[FORWARD] = CROWD_FROM + (CROWD_TO - CROWD_FROM) * eased;
        float stride = 1F - smoother((t - CROWD_WALK * 0.75F) / 0.3F);
        float swing = (float) Math.sin(t * 11F + phase) * 0.9F * stride;
        float cheer = smoother((t - CROWD_WALK) / 0.25F);
        float bounce = (float) Math.abs(Math.sin((t - CROWD_WALK) * 7.5F + phase));
        out[2 * 3] = -swing * (1 - cheer) - 2.85F * cheer + 0.25F * bounce * cheer;
        out[3 * 3] = swing * (1 - cheer) - 2.85F * cheer + 0.25F * (1 - bounce) * cheer;
        out[2 * 3 + 2] = 0.1F + 0.25F * cheer;
        out[3 * 3 + 2] = -0.1F - 0.25F * cheer;
        out[4 * 3] = swing * 1.1F;
        out[5 * 3] = -swing * 1.1F;
        out[0] = -0.25F * cheer;
        out[RISE] = cheer * bounce * 0.22F;
        return out;
    }

    static float smoother(float t) {
        float k = clamp01(t);
        return k * k * k * (k * (k * 6F - 15F) + 10F);
    }

    private static float clamp01(float v) {
        return v < 0F ? 0F : Math.min(v, 1F);
    }

    private static float[] pose(float... values) {
        if (values.length != WIDTH) throw new IllegalArgumentException("pose needs " + WIDTH + " values");
        return values;
    }

    private static float[] key(float time, float[] pose) {
        float[] key = new float[WIDTH + 1];
        key[0] = time;
        System.arraycopy(pose, 0, key, 1, WIDTH);
        return key;
    }

    /** A rest key that keeps the stepped-in distance (so the character settles in place, not back home). */
    private static float[] key(float time, float[] pose, float forward) {
        return key(time, pose, forward, 0F);
    }

    private static float[] key(float time, float[] pose, float forward, float spin) {
        float[] key = key(time, pose);
        key[1 + FORWARD] = forward;
        key[1 + SPIN] = spin;
        return key;
    }
}
