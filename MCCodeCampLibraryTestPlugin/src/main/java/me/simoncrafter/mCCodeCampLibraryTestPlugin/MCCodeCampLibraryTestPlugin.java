package me.simoncrafter.mCCodeCampLibraryTestPlugin;

import me.simoncrafter.mCCodeCampLibrary.utility.InitHelper;
import org.bukkit.plugin.java.JavaPlugin;

public final class MCCodeCampLibraryTestPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        InitHelper.init(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
