package me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.entranceDoor;

import me.simoncrafter.mCCodeCampLibrary.courseapi.ACourseSegment;
import me.simoncrafter.mCCodeCampLibrary.input.activation.event.playerBlockActivation.ButtonIDEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.events.BlockRegistryUpdateEvent;
import me.simoncrafter.mCCodeCampLibrary.obstical.Door.SlidingDoor;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import me.simoncrafter.mCCodeCampLibraryTestPlugin.MCCodeCampLibraryTestPlugin;
import me.simoncrafter.mCCodeCampLibraryTestPlugin.yourCode.entranceDoor.Course;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class EntranceDoorCourseSegment extends ACourseSegment {

    private final EntranceDoor student;
    private SlidingDoor door;

    public EntranceDoorCourseSegment() {
        student = new Course();
        student.setSegment(this);
        findObjects();
    }

    private void findObjects() {
        if (MCCodeCampLib.getBlockMarkerRegistry().findRegisteredObject("sliding_door", "entrance_door") instanceof SlidingDoor sDoor) {
            door = sDoor;
        }
    }

    @EventHandler
    public void onBlockRegistryUpdate(BlockRegistryUpdateEvent event) {
        findObjects();
    }

    @Override
    public String getSegmentID() {
        return "entrance_door";
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
        if (event.getID().startsWith("entrance_door_button")) {
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

    void sOpen() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcast(Component.text("Open"));
                door.open();
            }
        }.runTask(getPlugin());
    }
    void sClose() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcast(Component.text("Close"));
                door.close();
            }
        }.runTask(getPlugin());
    }

}
