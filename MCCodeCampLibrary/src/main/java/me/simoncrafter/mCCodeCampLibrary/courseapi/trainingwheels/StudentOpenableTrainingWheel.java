package me.simoncrafter.mCCodeCampLibrary.courseapi.trainingwheels;

import me.simoncrafter.mCCodeCampLibrary.obstical.IOpenable;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;

public class StudentOpenableTrainingWheel {

    private final Set<String> ALLOWED_OPENABLES;
    private final Plugin PLUGIN;

    public StudentOpenableTrainingWheel(Set<String> allowedOpenables, Plugin plugin) {
        this.ALLOWED_OPENABLES = allowedOpenables;
        this.PLUGIN = plugin;
    }


    /**
     * Handles opening the given openable. Checks if the ID is an allowed ID and performs the open command on the next tick
     * @param openableName The full type:id of the openable, also required in the allowed set
     */

    public void open(String openableName) {
        setDoorState(openableName, IOpenable::open);
    }

    public void close(String openableName) {
        setDoorState(openableName, IOpenable::open);
    }

    private void setDoorState(String openableName, Consumer<IOpenable> action) {
        new BukkitRunnable() {
            @Override
            public void run() {
                IOpenable door = getDoor(openableName);
                if (door != null) {
                    action.accept(door);
                }
            }
        }.runTask(PLUGIN);
    }

    private IOpenable getDoor(String openableName) {
        if (openableName == null) {
            return null;
        }
        String[] split = openableName.split(":", -1);
        if (split.length != 2 || split[0].isBlank() || split[1].isBlank()) {
            return null;
        }

        String type = split[0];
        String id = split[1];


        if (!ALLOWED_OPENABLES.contains(openableName)) {
            return null;
        }

        for (var object : MCCodeCampLib.getBlockMarkerRegistry()
                .getRegisteredObjects()) {
            if (object.getID().equals(id)
                    && object.getTypeID().equals(type)
                    && object instanceof IOpenable openable) {
                return openable;
            }
        }
        return null; // not found
    }
}
