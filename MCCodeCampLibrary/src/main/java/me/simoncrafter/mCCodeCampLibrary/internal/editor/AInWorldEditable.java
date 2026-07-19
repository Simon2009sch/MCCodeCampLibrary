package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import me.simoncrafter.CraftersDisplayLibrary.core.PositionObject;
import org.bukkit.Location;

public abstract class AInWorldEditable extends AEditable {

    protected abstract PositionObject spawnDisplay();
    protected abstract Location getLocation();


}
