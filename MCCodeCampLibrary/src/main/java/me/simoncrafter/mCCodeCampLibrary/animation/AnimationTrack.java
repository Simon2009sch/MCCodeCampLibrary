package me.simoncrafter.mCCodeCampLibrary.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.configurate.ConfigurationNode;
import java.util.ArrayList;
import java.util.List;

/** Absolute pivot-local keyframes. Each time is the travel duration from the preceding frame. */
public final class AnimationTrack {
    private record Frame(AnimationPose pose, Vector3f euler, int time) { }
    private final List<Frame> frames;
    private final long duration;
    private final boolean loop;

    private AnimationTrack(List<Frame> frames, boolean loop) {
        this.frames = List.copyOf(frames);
        this.duration = frames.stream().mapToLong(Frame::time).sum();
        this.loop = loop;
    }

    public static AnimationTrack read(ConfigurationNode config) {
        config = config.copy(); // Configurate default getters may attach nodes; parsing must not edit saved JSON.
        List<Frame> frames = new ArrayList<>();
        if (config.node("animation").childrenList().size() > 1024) throw new IllegalArgumentException("Too many keyframes");
        for (ConfigurationNode n : config.node("animation").childrenList()) {
            double rawTime = n.node("time").getDouble(-1);
            if (!Double.isFinite(rawTime) || rawTime < 1 || rawTime > 72000 || rawTime != Math.rint(rawTime))
                throw new IllegalArgumentException("Keyframe time must be an integer from 1 to 72000 ticks");
            Vector3f scale = vector(n.node("scale"), 1);
            if (scale.x <= 0 || scale.y <= 0 || scale.z <= 0) throw new IllegalArgumentException("Scale must be positive");
            Vector3f euler = n.node("rotation").virtual() ? null : vector(n.node("rotation"), 0);
            if (euler != null && !n.node("leftRotation").virtual()) throw new IllegalArgumentException("Use rotation OR leftRotation");
            frames.add(new Frame(new AnimationPose(vector(n.node("translation"), 0), quaternion(n.node("leftRotation")),
                    scale, quaternion(n.node("rightRotation"))), euler, (int) rawTime));
        }
        return new AnimationTrack(frames, config.node("loop").getBoolean(true));
    }

    /** Samples server tick time; Euler degrees are interpolated unwrapped, preserving complete turns. */
    public AnimationPose at(long tick) {
        if (frames.isEmpty()) return AnimationPose.identity();
        long t = Math.max(0, tick);
        if (loop) t %= duration;
        Frame previous = new Frame(AnimationPose.identity(), new Vector3f(), 0);
        for (Frame next : frames) {
            if (t < next.time()) return interpolate(previous, next, (float) t / next.time());
            t -= next.time();
            previous = next;
        }
        return interpolate(previous, previous, 1);
    }

    private static AnimationPose interpolate(Frame a, Frame b, float t) {
        Quaternionf left;
        if (a.euler() != null && b.euler() != null) {
            Vector3f e = new Vector3f(a.euler()).lerp(b.euler(), t).mul((float) (Math.PI / 180));
            left = new Quaternionf().rotationXYZ(e.x, e.y, e.z);
        } else {
            left = rotation(a).slerp(rotation(b), t);
        }
        return new AnimationPose(new Vector3f(a.pose().translation()).lerp(b.pose().translation(), t), left,
                new Vector3f(a.pose().scale()).lerp(b.pose().scale(), t),
                new Quaternionf(a.pose().rightRotation()).slerp(b.pose().rightRotation(), t));
    }

    private static Quaternionf rotation(Frame frame) {
        if (frame.euler() == null) return new Quaternionf(frame.pose().leftRotation());
        Vector3f r = new Vector3f(frame.euler()).mul((float) (Math.PI / 180));
        return new Quaternionf().rotationXYZ(r.x, r.y, r.z);
    }

    public static Vector3f vector(ConfigurationNode node, float fallback) {
        return new Vector3f(number(node.node("x"), fallback), number(node.node("y"), fallback), number(node.node("z"), fallback));
    }

    private static Quaternionf quaternion(ConfigurationNode node) {
        Quaternionf q = new Quaternionf(number(node.node("x"), 0), number(node.node("y"), 0),
                number(node.node("z"), 0), number(node.node("w"), 1));
        if (q.lengthSquared() < .000001f) throw new IllegalArgumentException("Rotation quaternion must not be zero");
        return q.normalize();
    }

    private static float number(ConfigurationNode n, float fallback) {
        float f = n.getFloat(fallback);
        if (!Float.isFinite(f) || Math.abs(f) > 30000000) throw new IllegalArgumentException("Transform contains a nonfinite/out-of-range number");
        return f;
    }
}
