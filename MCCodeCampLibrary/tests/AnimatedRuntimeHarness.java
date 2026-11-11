package me.simoncrafter.mCCodeCampLibrary.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/** Tests the actual tick controller through its world/display boundary, not a Minecraft server. */
public class AnimatedRuntimeHarness {
    static class Scene implements AnimatedBlockRuntime.Scene {
        boolean original = true, loaded = true;
        int displays, restores, clears, displayCalls;
        public int size() { return 1; }
        public Vector3f originalCenter(int index) { return new Vector3f(.5f); }
        public boolean loaded(int index, Vector3f center) { return loaded; }
        public void original(int index) { original=true; restores++; }
        public void replacement(int index) { original=false; clears++; }
        public void display(int index, Vector3f center, Quaternionf left, Vector3f scale, Quaternionf right) { displays=1; displayCalls++; }
        public void removeDisplay(int index) { displays=0; }
    }
    public static void main(String[] args) {
        Scene scene = new Scene();
        AnimatedBlockRuntime runtime = new AnimatedBlockRuntime(scene);
        var storage = List.of(new Vector3f(15, 2, 0));
        runtime.proximity(List.of(new Vector3f(20, .5f, .5f)), storage, 3, 10, 50, 3);
        if (scene.original || scene.displays!=1) throw new AssertionError("Did not assemble into storage");
        int settledCalls=scene.displayCalls;
        runtime.proximity(List.of(new Vector3f(25,.5f,.5f)),storage,3,10,50,3);
        if(scene.displayCalls!=settledCalls)throw new AssertionError("Settled storage pose resent while player remains beyond ten blocks");
        runtime.proximity(List.of(new Vector3f(6,.5f,.5f)),storage,3,10,50,3);
        if(scene.displayCalls!=settledCalls+1)throw new AssertionError("Moving pose not updated");
        runtime.proximity(List.of(new Vector3f(6,.5f,.5f)),storage,3,10,50,3);
        if(scene.displayCalls!=settledCalls+1)throw new AssertionError("Stationary intermediate pose resent");
        runtime.proximity(List.of(new Vector3f(.5f)), storage, 3, 10, 50, 3);
        if (!scene.original || scene.displays!=0) throw new AssertionError("Near state must be a real block");
        runtime.proximity(List.of(new Vector3f(20, .5f, .5f)), storage, 3, 10, 50, 3);
        runtime.proximity(List.of(), storage, 3, 10, 50, 3);
        if (!scene.original || scene.displays!=0) throw new AssertionError("Far state must restore schematic");
        runtime.proximity(List.of(new Vector3f(20, .5f, .5f)), storage, 3, 10, 50, 3);
        runtime.close(); runtime.close();
        int mutations = scene.clears;
        runtime.proximity(List.of(new Vector3f(20, .5f, .5f)), storage, 3, 10, 50, 3);
        if (!scene.original || scene.displays!=0 || mutations!=scene.clears) throw new AssertionError("Closed runtime remains active");
        Scene faulty=new Scene() {
            public int size(){return 2;}
            public void original(int i){if(i==0)throw new IllegalStateException("simulated restore failure");restores++;}
        };
        AnimatedBlockRuntime failedRuntime=new AnimatedBlockRuntime(faulty);
        try {failedRuntime.close();throw new AssertionError("Restore failure swallowed");}catch(IllegalStateException expected){}
        if(faulty.restores!=1)throw new AssertionError("Cleanup stopped after the first failed block");
        failedRuntime.close();
        if(faulty.restores!=1)throw new AssertionError("Failed close was not idempotent");
        System.out.println("AnimatedRuntimeHarness passed");
    }
}
