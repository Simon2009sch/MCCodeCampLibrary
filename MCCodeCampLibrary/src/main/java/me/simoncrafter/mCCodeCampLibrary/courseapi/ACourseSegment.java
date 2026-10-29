package me.simoncrafter.mCCodeCampLibrary.courseapi;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginBase;

public abstract class ACourseSegment implements Listener {


    private Plugin PLUGIN;


    void setPlugin(Plugin plugin) {
        PLUGIN = plugin;
    }


    /**
     * Method gets called every in game tick (20 times per second) as long as the course segment is active
     */
    protected abstract void onTick();

    /**
     * Gets called when the course segment is activated. Meaning the course manager has deemed this segment to be active
     * and in use by the student. (Maybe triggered by some regional trigger)
     */
    protected abstract void onActivate();

    /**
     * Gets called when the course segment is deactivated. Meaning the course manager has deemed this segment to be
     * no longer in use by the student. (Maybe triggered by some regional trigger)
     */
    protected abstract void onDeactivate();

    public abstract String getSegmentID();

    protected final Plugin getPlugin() {
        return PLUGIN;
    }

}
