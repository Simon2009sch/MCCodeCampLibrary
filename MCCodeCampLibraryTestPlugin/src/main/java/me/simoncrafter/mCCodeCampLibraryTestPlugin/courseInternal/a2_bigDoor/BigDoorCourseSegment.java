package me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.a2_bigDoor;

import me.simoncrafter.mCCodeCampLibrary.courseapi.ACourseSegment;
import me.simoncrafter.mCCodeCampLibrary.courseapi.trainingwheels.StudentOpenableTrainingWheel;
import me.simoncrafter.mCCodeCampLibrary.input.activation.event.playerBlockActivation.ButtonIDEvent;
import me.simoncrafter.mCCodeCampLibraryTestPlugin.yourCode.a2_bigDoor.Course;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

public class BigDoorCourseSegment extends ACourseSegment {

    private final BigDoor student;
    private final StudentOpenableTrainingWheel doorWheel;


    public BigDoorCourseSegment(Plugin plugin) {
        super(plugin);
        student = new Course();
        student.setSegment(this);
        doorWheel = new StudentOpenableTrainingWheel(Set.of("sliding_door:big_door_right", "sliding_door:big_door_left"), getPlugin());
    }




    @Override
    public String getSegmentID() {
        return "big_door";
    }

    @Override
    protected final void onTick() {

    }

    @Override
    protected final void onActivate() {

    }

    @Override
    protected final void onDeactivate() {

    }

    @EventHandler
    public void onButtonID(ButtonIDEvent event) {
        if (event.getID().startsWith("big_door_button")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        student.onButtonPress();
                    } catch (InterruptedException ignored) {

                    }
                }
            }.runTaskAsynchronously(getPlugin());
        }
    }

    void sOpen(String doorName) {
        doorWheel.open(doorName);
    }
    void sClose(String doorName) {
        doorWheel.open(doorName);
    }

}
