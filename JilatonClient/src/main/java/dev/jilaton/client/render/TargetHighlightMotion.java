package dev.jilaton.client.render;

/** Time-based target acquisition, tracking, fade-out and restrained scan motion. No world references retained. */
public final class TargetHighlightMotion {
    private Target previous;
    private double opacity;
    private long lastTime;
    private boolean initialized;

    public State update(Target target, long now, boolean reducedMotion) {
        double dt = initialized ? Math.max(0, Math.min(0.1, (now - lastTime) / 1_000_000_000.0)) : 0;
        lastTime = now;
        initialized = true;
        if (target != null) {
            if (previous == null || previous.id() != target.id() || distanceSquared(previous, target) > 16) {
                previous = target;
                opacity = reducedMotion ? 1 : 0;
                dt = 0; // The time before acquisition belongs to the previous target, not this fade-in.
            } else {
                double ease = reducedMotion ? 1 : -Math.expm1(-24 * dt);
                previous = new Target(target.id(), lerp(previous.x(), target.x(), ease), lerp(previous.y(), target.y(), ease),
                        lerp(previous.z(), target.z(), ease), target.height(), target.radius());
            }
        }
        opacity = reducedMotion ? (target == null ? 0 : 1)
                : lerp(opacity, target == null ? 0 : 1, -Math.expm1(-14 * dt));
        if (previous == null || opacity < 0.005 && target == null) { previous = null; return null; }
        double phase = reducedMotion ? Math.PI / 2 : now / 1_000_000_000.0 * 1.55;
        double scan = reducedMotion ? 0.52 : 0.12 + (0.5 - 0.5 * Math.cos(phase)) * 0.76;
        return new State(previous, opacity, scan, reducedMotion ? 0 : Math.sin(phase), reducedMotion ? 0 : phase);
    }

    public void reset() { previous = null; opacity = 0; initialized = false; }
    private static double distanceSquared(Target a, Target b) { return Math.pow(a.x()-b.x(),2)+Math.pow(a.y()-b.y(),2)+Math.pow(a.z()-b.z(),2); }
    private static double lerp(double a, double b, double factor) { return a + (b - a) * factor; }
    public record Target(int id, double x, double y, double z, double height, double radius) { }
    public record State(Target target, double opacity, double scan, double direction, double phase) { }
}
