package me.simoncrafter.mCCodeCampLibraryTestPlugin;

import me.simoncrafter.mCCodeCampLibrary.commands.CourseEditCommand;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class MCCodeCampLibraryTestPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        MCCodeCampLib.init(this);
        Bukkit.getPluginCommand("coursedit").setExecutor(new CourseEditCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
