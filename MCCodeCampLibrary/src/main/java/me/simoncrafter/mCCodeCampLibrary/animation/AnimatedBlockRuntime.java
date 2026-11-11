package me.simoncrafter.mCCodeCampLibrary.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.Arrays;
import java.util.List;

/** Shared main-thread controller; owns display/source transitions, but no scheduler or persistence. */
public final class AnimatedBlockRuntime implements AutoCloseable {
    public interface Scene {
        int size();
        Vector3f originalCenter(int index);
        boolean loaded(int index, Vector3f displayCenter);
        void original(int index);
        void replacement(int index);
        void display(int index, Vector3f center, Quaternionf left, Vector3f scale, Quaternionf right);
        void removeDisplay(int index);
    }

    private final Scene scene;
    // -1 unknown (e.g. after crash), 0 replacement, 1 original. First access restores authoritative data.
    private final byte[] states;
    private final Vector3f[] originals;
    private final Quaternionf[] storageRotations;
    private final float[] lastProgress;
    private boolean closed;

    public AnimatedBlockRuntime(Scene scene) {
        this.scene = scene;
        states = new byte[scene.size()];
        Arrays.fill(states, (byte)-1);
        originals=new Vector3f[states.length];storageRotations=new Quaternionf[states.length];
        lastProgress=new float[states.length];Arrays.fill(lastProgress,Float.NaN);
        for(int i=0;i<states.length;i++)originals[i]=new Vector3f(scene.originalCenter(i));
    }

    public void timed(AnimationPose pose, Vector3f pivot) {
        if (closed) return;
        for (int i=0; i<states.length; i++) {
            Vector3f original = originals[i];
            Vector3f offset = original.sub(pivot, new Vector3f());
            pose.rightRotation().transform(offset);
            offset.mul(pose.scale());
            pose.leftRotation().transform(offset);
            Vector3f center = offset.add(pivot).add(pose.translation());
            if (!scene.loaded(i, center)) { restore(i); continue; }
            replace(i);
            scene.display(i, center, pose.leftRotation(), pose.scale(), pose.rightRotation());
        }
    }

    /** Checks squared distance each tick; settled blocks do no path/rotation math and send no display update. */
    public void proximity(List<Vector3f> players, List<Vector3f> storage, double near, double far, double range, float curveHeight) {
        if (closed) return;
        if (storage.size() != states.length) throw new IllegalArgumentException("Storage layout size differs from schematic");
        double nearSquared=near*near,farSquared=far*far,rangeSquared=range*range;
        for (int i=0; i<states.length; i++) {
            Vector3f original=originals[i];
            double distanceSquared=Double.POSITIVE_INFINITY;
            for (Vector3f player:players)distanceSquared=Math.min(distanceSquared,original.distanceSquared(player));
            if(distanceSquared>rangeSquared||distanceSquared<=nearSquared){restore(i);continue;}
            float progress=distanceSquared>=farSquared?0:ProximityMath.progress(Math.sqrt(distanceSquared),near,far);
            if(states[i]==0 && progress==lastProgress[i])continue;
            Vector3f center=progress==0?storage.get(i):ProximityMath.curve(storage.get(i),original,progress,curveHeight);
            if(!scene.loaded(i,center)){restore(i);continue;}
            if(storageRotations[i]==null) {
                Vector3f angles=ProximityMath.storageAngles((int)Math.floor(original.x),(int)Math.floor(original.y),(int)Math.floor(original.z));
                angles.mul((float)(Math.PI/180));
                storageRotations[i]=new Quaternionf().rotationXYZ(angles.x,angles.y,angles.z);
            }
            Quaternionf rotation=progress==0?storageRotations[i]:new Quaternionf(storageRotations[i]).slerp(new Quaternionf(),progress);
            replace(i);
            scene.display(i,center,rotation,new Vector3f(1),new Quaternionf());
            lastProgress[i]=progress;
        }
    }

    private void replace(int i) {
        if (states[i] != 0) { scene.replacement(i); states[i]=0; }
    }

    private void restore(int i) {
        lastProgress[i]=Float.NaN;
        if(states[i]==1)return;
        scene.removeDisplay(i);
        // The adapter must never force-load a source chunk during cleanup.
        if (states[i] != 1) { scene.original(i); states[i]=1; }
    }

    /** Restores all loaded sources and removes every owned display, safe to call repeatedly. */
    public void suspend() {
        if (closed) return;
        RuntimeException failure=null;
        for (int i=0; i<states.length; i++) {
            try { restore(i); } catch(RuntimeException e) {
                if(failure==null)failure=e;else failure.addSuppressed(e);
            }
        }
        Arrays.fill(states, (byte)-1);
        if(failure!=null)throw failure;
    }

    @Override public void close() {
        if (closed) return;
        try { suspend(); } finally { closed=true; }
    }
}
