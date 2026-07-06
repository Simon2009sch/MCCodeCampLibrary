package me.simoncrafter.mCCodeCampLibrary.utility;

import me.simoncrafter.mCCodeCampLibrary.input.button.ButtonHandler;
import me.simoncrafter.mCCodeCampLibrary.internal.BlockMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.internal.Listeners;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class InitHelper {

    private static Plugin plugin;
    private static BlockMarkerRegistry blockMarkerRegistry;
    private static ButtonHandler buttonHandler;

    public static Plugin getPlugin() {
        return plugin;
    }

    public static BlockMarkerRegistry getBlockMarkerRegistry() {
        return blockMarkerRegistry;
    }

    public static ButtonHandler getButtonHandler() {
        return buttonHandler;
    }

    public static void init(@NotNull Plugin plugin) {
        if (InitHelper.plugin != null) {
            return;
        }
        InitHelper.plugin = plugin;
        registerEventListeners(plugin);
        InitHelper.blockMarkerRegistry = new BlockMarkerRegistry(plugin);
        InitHelper.buttonHandler = new ButtonHandler(plugin, blockMarkerRegistry);
    }

    private static void registerEventListeners(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new Listeners(), plugin);
    }

}
