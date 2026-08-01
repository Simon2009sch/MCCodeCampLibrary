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
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Set;

public class SlidingDoor extends AAnimatedOpenableObject {

    private Vector openPosition;

    @Override
    protected void readConfig() {
        super.readConfig();
        ConfigurationNode config = getConfig();

        openPosition = readVector(config.node("openPosition"), 0, 0, 0);

        openAnimationFunction = (obj, duration) -> {
            obj.scaleAbsolute(new Vector3f(0.999f, 0.999f, 0.999f), 1);
            obj.moveAbsolute(openPosition.toVector3f(), duration);
        };
        closeAnimationFunction = (obj, duration) -> {
            obj.moveAbsolute(new Vector3f(0, 0, 0), duration);
        };
    }

    public Vector getOpenPosition() {
        return openPosition.clone();
    }

    public void setOpenPosition(Vector openPosition) {
        this.openPosition = openPosition;
        try {
            getConfig().node("openPosition").from(writeVector(openPosition));
        } catch (SerializationException e) {
            getPlugin().getLogger().warning("There was an error while trying to serialize a vector into config!");
        }
    }
}
