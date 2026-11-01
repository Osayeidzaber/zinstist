/* Adapted from AimAssistModule.nudge, Horcus / 4ldenz, GPL-3.0-only. See ATTRIBUTION.md. */
package dev.jilaton.client.module.impl;

/** Fractional mouse-count accumulation and same-direction assistance from the reference implementation. */
public final class MouseAimState {
    public record Nudge(double yaw, double pitch) { }
    static final Nudge STILL = new Nudge(0, 0);
    private long movedAt;
    private boolean moved;
    private double userYawSign, userPitchSign, pendingYaw, pendingPitch;

    void resetCarry() { pendingYaw = pendingPitch = 0; }
    void reset() { resetCarry(); moved = false; userYawSign = userPitchSign = 0; }

    Nudge nudge(double userYaw, double userPitch, double yawError, double pitchError, boolean aiming,
                double seconds, double speed, double pace, double mouseScale, long nowMillis) {
        if (userYaw != 0 || userPitch != 0) { movedAt = nowMillis; moved = true; }
        if (userYaw != 0) userYawSign = Math.signum(userYaw);
        if (userPitch != 0) userPitchSign = Math.signum(userPitch);
        if (!aiming || !moved || nowMillis - movedAt > 150 || !Double.isFinite(seconds)
                || !Double.isFinite(yawError) || !Double.isFinite(pitchError)
                || !Double.isFinite(mouseScale) || mouseScale <= 0) {
            resetCarry(); userYawSign = userPitchSign = 0;
            return STILL;
        }
        double dt = Math.max(0, Math.min(seconds, .05));
        if (userYawSign * yawError < 0) yawError = 0;
        if (userPitchSign * pitchError < 0) pitchError = 0;
        double cap = speed * pace * dt;
        double ease = 1 - Math.exp(-12 * dt);
        double degreesPerCount = (float) mouseScale * .15f;
        pendingYaw += Math.clamp(yawError * ease, -cap, cap) / degreesPerCount;
        pendingPitch += Math.clamp(pitchError * ease * .6, -cap, cap) / degreesPerCount;
        long yawCounts = (long) pendingYaw, pitchCounts = (long) pendingPitch;
        pendingYaw -= yawCounts; pendingPitch -= pitchCounts;
        return new Nudge(yawCounts * mouseScale, pitchCounts * mouseScale);
    }
}
