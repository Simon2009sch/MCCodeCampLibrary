package me.simoncrafter.mCCodeCampLibrary.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Pivot-local display transform; callers own the mutable math values. */
public record AnimationPose(Vector3f translation, Quaternionf leftRotation, Vector3f scale, Quaternionf rightRotation) {
    public static AnimationPose identity() {
        return new AnimationPose(new Vector3f(), new Quaternionf(), new Vector3f(1), new Quaternionf());
    }
}
