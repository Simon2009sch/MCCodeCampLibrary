package me.simoncrafter.mCCodeCampLibrary.obstical.Door;

import me.simoncrafter.mCCodeCampLibrary.obstical.AAnimatedOpenableObject;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.joml.Quaternionf;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class PivotingDoor extends AAnimatedOpenableObject {

    private float openAngle;

    @Override
    protected void readConfig() {
        super.readConfig();
        ConfigurationNode config = getConfig();

        openAngle = config.node("openAngle").getFloat(90);

        openAnimationFunction = (obj, duration) -> {
            obj.LRotateAbsolute(rotationFromYaw(this.openAngle), duration);
            Bukkit.broadcast(Component.text("Pivoting open"));
        };
        closeAnimationFunction = (obj, duration) -> {
            obj.LRotateAbsolute(new Quaternionf(0, 0, 0, 1), duration);
            Bukkit.broadcast(Component.text("Pivoting closed"));
        };
    }

    public float getOpenAngle() {
        return openAngle;
    }

    public void setOpenAngle(float openAngle) {
        this.openAngle = openAngle;
        try {
            getConfig().node("openAngle").set(openAngle);
        } catch (SerializationException e) {
            getPlugin().getLogger().warning("There was an error while trying to serialize openAngle into config!");
        }
    }

    public static Quaternionf rotationFromYaw(float degrees) {
        return new Quaternionf().rotateY((float) Math.toRadians(degrees));
    }
}
