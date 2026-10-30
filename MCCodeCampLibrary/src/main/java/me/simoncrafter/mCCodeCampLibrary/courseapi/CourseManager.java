package me.simoncrafter.mCCodeCampLibrary.courseapi;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public final class CourseManager {
    private final Plugin PLUGIN;



    private Map<String, ACourseSegment> activeSegments = new HashMap<>();

    public CourseManager(Plugin PLUGIN) {
        this.PLUGIN = PLUGIN;
    }

    public void onTick() {
        for (ACourseSegment s : activeSegments.values()) {
            s.onTick();
        }
    }



    private void _activateCourse(ACourseSegment segment) {
        activeSegments.put(segment.getSegmentID(), segment);
        Bukkit.getPluginManager().registerEvents(segment, PLUGIN);
        segment.onActivate();
    }

    private void _deactivateCourse(ACourseSegment segment) {
        activeSegments.remove(segment.getSegmentID());
        HandlerList.unregisterAll(segment);
        segment.onDeactivate();
    }


    public void registerSegment(ACourseSegment segment) throws IllegalStateException {
        if (activeSegments.containsKey(segment.getSegmentID())) {
            throw new IllegalStateException("Segment id \"" + segment.getSegmentID() + "\" is already registered! Use unique IDs!");
        }
        _activateCourse(segment);
    }

    public void unregisterSegment(ACourseSegment segment) {
        if (segment == null || !activeSegments.containsValue(segment)) {
            return;
        }
        _deactivateCourse(segment);
    }

    public void unregisterSegment(String segmentID) {
        unregisterSegment(activeSegments.get(segmentID));
    }


}
