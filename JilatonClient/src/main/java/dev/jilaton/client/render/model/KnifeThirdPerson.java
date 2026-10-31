package dev.jilaton.client.render.model;

/**
 * Third-person attachment for the knife: takes the wrist frame from {@link BodyAnchor} and lays the same
 * {@link KnifeMotion} pose on top of it in hand space. First-person offsets are view-space blocks; a third-person
 * hand is smaller than the camera's, so translation is scaled down while the rotations stay 1:1 so draw, slash and
 * inspect read the same from outside as they do from inside.
 */
public final class KnifeThirdPerson {
    /** How much of the first-person translation survives in a hand-sized frame. */
    public static final double TRANSLATION = 0.25;

    private KnifeThirdPerson() { }

    public static Transform hand(Transform wrist, KnifeMotion.Pose pose) {
        Transform placed = wrist.translate(pose.x() * TRANSLATION, pose.y() * TRANSLATION, pose.z() * TRANSLATION);
        double yaw = pose.yaw(), pitch = pose.pitch(), roll = pose.roll() + pose.spin();
        // Use a proper local rotation: Transform.rotation builds a Minecraft-shaped frame and composing it
        // onto the wrist would mirror the blade's right axis (and pop 180 degrees once the pose left rest).
        if (Math.abs(yaw) + Math.abs(pitch) + Math.abs(roll) < 1e-9) return placed;
        return placed.spin(yaw, pitch, roll);
    }
}
