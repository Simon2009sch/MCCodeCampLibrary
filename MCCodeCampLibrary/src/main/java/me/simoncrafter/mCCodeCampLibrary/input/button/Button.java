package me.simoncrafter.mCCodeCampLibrary.input.button;

import me.simoncrafter.mCCodeCampLibrary.utility.Cooldown;
import org.bukkit.Location;

/**
 * A registered button location and its press cooldown. Dispatch (the callback that
 * runs on press) lives in {@link ButtonHandler}, not here — subclass this only if a
 * button "kind" needs its own extra state.
 */
public abstract class Button {
    private final Location location;
    private final String id;
    private final Cooldown cooldown;

    protected Button(Location location, String id, Cooldown cooldown) {
        this.location = location;
        this.id = id;
        this.cooldown = cooldown;
    }

    public Location getLocation() {
        return location;
    }

    public String getId() {
        return id;
    }

    public Cooldown getCooldown() {
        return cooldown;
    }
}
