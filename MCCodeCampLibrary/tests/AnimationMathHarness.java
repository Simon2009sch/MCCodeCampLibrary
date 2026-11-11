package me.simoncrafter.mCCodeCampLibrary.animation;

import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.joml.Vector3f;

/** Real transform math, without a server or rendering doubles. */
public class AnimationMathHarness {
    private static void near(float actual, float expected) {
        if (Math.abs(actual - expected) > .001f) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) throws Exception {
        ConfigurationNode config = BasicConfigurationNode.root();
        ConfigurationNode frame = config.node("animation").appendListNode();
        frame.node("time").set(20);
        frame.node("translation", "x").set(4);
        frame.node("rotation", "z").set(360);
        frame.node("scale", "x").set(3);
        AnimationTrack track = AnimationTrack.read(config);
        near(track.at(10).translation().x, 2);
        near(track.at(10).scale().x, 2);
        Vector3f rotated = track.at(10).leftRotation().transform(new Vector3f(1, 0, 0));
        near(rotated.x, -1); // A full turn must not collapse to identity quaternion interpolation.
        near(track.at(20).translation().x, 0);
        config.node("loop").set(false);
        near(AnimationTrack.read(config).at(100).translation().x, 4);
        frame.node("time").set(0);
        try { AnimationTrack.read(config); throw new AssertionError("Accepted zero-duration keyframe"); }
        catch (IllegalArgumentException expected) { }
        near(ProximityMath.progress(10, 3, 10), 0);
        near(ProximityMath.progress(3, 3, 10), 1);
        near(ProximityMath.progress(6.5, 3, 10), .5f);
        Vector3f start = new Vector3f(20, 0, 0), end = new Vector3f(0, 0, 0);
        near(ProximityMath.curve(start, end, 0, 4).x, 20);
        near(ProximityMath.curve(start, end, 1, 4).x, 0);
        if (ProximityMath.curve(start, end, .5f, 4).y <= 0) throw new AssertionError("Path is not curved");
        Vector3f angles = ProximityMath.storageAngles(4, 5, 6);
        if (!angles.equals(ProximityMath.storageAngles(4, 5, 6))) throw new AssertionError("Unstable storage rotation");
        if (angles.x < -100 || angles.x > 100) throw new AssertionError("Rotation out of range");
        boolean negative=false;
        for(int i=0;i<20;i++)if(ProximityMath.storageAngles(i,2,3).x<0)negative=true;
        if(!negative)throw new AssertionError("Bridge storage rotations never use negative angles");
        var positions = ProximityMath.scatter(8, new Vector3f(0), new Vector3f(4), 42);
        for (int i=0; i<positions.size(); i++) for (int j=0; j<i; j++)
            if (positions.get(i).distance(positions.get(j)) < 1) throw new AssertionError("Storage blocks overlap");
        try { ProximityMath.scatter(1000, new Vector3f(0), new Vector3f(1), 42); throw new AssertionError("Accepted insufficient storage"); }
        catch (IllegalArgumentException expected) { }
        ConfigurationNode settingsNode = BasicConfigurationNode.root();
        settingsNode.node("blocks").appendListNode().node("min", "x").set(10);
        settingsNode.node("blocks", 0, "max", "x").set(12);
        AnimationSettings settings = AnimationSettings.read(settingsNode, new Vector3f(3,4,5));
        near(settings.pivot().x, 3);
        if (settings.regions().size()!=1) throw new AssertionError("Missing source selection");
        settingsNode.node("nearDistance").set(11);
        try { AnimationSettings.read(settingsNode, new Vector3f()); throw new AssertionError("Accepted reversed thresholds"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("AnimationMathHarness passed");
    }
}
