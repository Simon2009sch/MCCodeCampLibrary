package me.simoncrafter.mCCodeCampLibraryTestPlugin;

import me.simoncrafter.mCCodeCampLibrary.commands.CourseEditCommand;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.a1_entranceDoor.EntranceDoorCourseSegment;
import me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.a2_bigDoor.BigDoorCourseSegment;
import net.kyori.adventure.text.Component;import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public final class MCCodeCampLibraryTestPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        MCCodeCampLib.init(this);
        Bukkit.getPluginCommand("coursedit").setExecutor((CommandExecutor) new CourseEditCommand(this));
        Bukkit.broadcast(Component.text("Reloaded!!"));
        MCCodeCampLib.getCourseManager().registerSegment(new EntranceDoorCourseSegment(this));
        MCCodeCampLib.getCourseManager().registerSegment(new BigDoorCourseSegment(this));

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        MCCodeCampLib.onDisable();
    }
}