package me.simoncrafter.mCCodeCampLibrary.obstical.Door;

import me.simoncrafter.mCCodeCampLibrary.obstical.AAnimatedOpenableObject;
import me.simoncrafter.mCCodeCampLibrary.obstical.OpenableState;
import me.simoncrafter.mCCodeCampLibrary.obstical.events.OpenableObjectStateChangeEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Set;

public class SlidingDoor extends AAnimatedOpenableObject {

    private Vector openPosition;

    public SlidingDoor(String ID, Location origen, Set<Vector> blocks, int animationDuration, Vector pivotPosition, boolean addCollision, Vector openPosition) {
        super(ID, origen, blocks, animationDuration, pivotPosition, addCollision);
        this.openPosition = openPosition;
        openAnimationFunction = (obj, duration) -> {
            obj.scaleAbsolute(new Vector3f(0.999f, 0.999f, 0.999f), 1);
            obj.moveAbsolute(openPosition.toVector3f(), duration);
        };
        closeAnimationFunction = (obj, duration) -> {
            obj.moveAbsolute(new Vector3f(0, 0, 0), duration);
        };
    }
}
