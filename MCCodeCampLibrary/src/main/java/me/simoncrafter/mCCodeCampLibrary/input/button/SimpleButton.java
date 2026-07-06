package me.simoncrafter.mCCodeCampLibrary.input.button;

import me.simoncrafter.mCCodeCampLibrary.utility.Cooldown;
import org.bukkit.Location;

/** Plain, ready-to-use Button for course content that doesn't need a custom subclass. */
public class SimpleButton extends Button {
    public SimpleButton(Location location, String id, Cooldown cooldown) {
        super(location, id, cooldown);
    }
}
