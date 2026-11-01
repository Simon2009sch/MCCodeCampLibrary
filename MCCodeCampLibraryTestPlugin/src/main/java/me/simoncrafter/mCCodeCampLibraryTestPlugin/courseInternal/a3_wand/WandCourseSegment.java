package me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.a3_wand;

import me.simoncrafter.mCCodeCampLibrary.courseapi.ACourseSegment;
import me.simoncrafter.mCCodeCampLibrary.courseapi.trainingwheels.StudentOpenableTrainingWheel;
import me.simoncrafter.mCCodeCampLibrary.input.activation.event.playerBlockActivation.ButtonIDEvent;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;

import me.simoncrafter.mCCodeCampLibraryTestPlugin.yourCode.a3_wand.Course;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

public class WandCourseSegment extends ACourseSegment {

    private final Course student;

    public WandCourseSegment(Plugin plugin) {
        super(plugin);
        student = new Course();
        MCCodeCampLib.getItemsManager().registerCustomItem(student);
    }


    @EventHandler
    public void onButtonID(ButtonIDEvent event) {
        if (event.getID().equals("get_wand")) {
            event.getPlayer().getInventory().addItem(MCCodeCampLib.getItemsManager().createNewItem(student));
        }
    }


    @Override
    public String getSegmentID() {
        return "wand";
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




}
