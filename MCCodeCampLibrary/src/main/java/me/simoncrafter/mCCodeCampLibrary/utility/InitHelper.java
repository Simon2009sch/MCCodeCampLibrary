package me.simoncrafter.mCCodeCampLibrary.utility;

import me.simoncrafter.mCCodeCampLibrary.internal.BlockMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.internal.Listeners;
import me.simoncrafter.mCCodeCampLibrary.internal.activation.ActivationListeners;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class InitHelper {

    private static Plugin plugin;
    private static BlockMarkerRegistry blockMarkerRegistry;
    public static Plugin getPlugin() {
        return plugin;
    }

    public static BlockMarkerRegistry getBlockMarkerRegistry() {
        return blockMarkerRegistry;
    }

    public static void init(@NotNull Plugin plugin) {
        if (InitHelper.plugin != null) {
            return;
        }
        InitHelper.plugin = plugin;
        registerEventListeners(plugin);
        InitHelper.blockMarkerRegistry = new BlockMarkerRegistry(plugin);
    }

    private static void registerEventListeners(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new Listeners(), plugin);
        Bukkit.getPluginManager().registerEvents(new ActivationListeners(), plugin);
    }

}
