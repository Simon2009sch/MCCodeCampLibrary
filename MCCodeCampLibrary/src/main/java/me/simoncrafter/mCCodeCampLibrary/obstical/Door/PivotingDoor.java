package me.simoncrafter.mCCodeCampLibrary.obstical.Door;

import me.simoncrafter.mCCodeCampLibrary.obstical.AAnimatedOpenableObject;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import java.util.Set;

public class PivotingDoor extends AAnimatedOpenableObject {

    private float openAngle;

    public PivotingDoor(String ID, Location origen, Set<Vector> blocks, int animationDuration, Vector pivotPosition, boolean addCollision, float openAngle) {
        super(ID, origen, blocks, animationDuration, pivotPosition, addCollision);
        this.openAngle = openAngle;
        this.openAnimationFunction = (obj, duration) -> {
            obj.LRotateAbsolute(rotationFromYaw(this.openAngle), duration);
            Bukkit.broadcast(Component.text("Pivoting open"));
        };
        this.closeAnimationFunction = (obj, duration) -> {
            obj.LRotateAbsolute(new Quaternionf(0, 0, 0, 1),  duration);
            Bukkit.broadcast(Component.text("Pivoting closed"));
        };
    }


    public static Quaternionf rotationFromYaw(float degrees) {
        return new Quaternionf().rotateY((float) Math.toRadians(degrees));
    }
}
