package me.simoncrafter.mCCodeCampLibrary.utility;

import me.simoncrafter.CraftersChatDialogs.InstanceData;
import me.simoncrafter.CraftersDisplayLibrary.PluginHolder;
import me.simoncrafter.mCCodeCampLibrary.internal.BlockMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.internal.EntityMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.internal.Listeners;
import me.simoncrafter.mCCodeCampLibrary.internal.activation.ActivationListeners;
import me.simoncrafter.mCCodeCampLibrary.internal.activation.RegionActivationHandler;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.EditorListener;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class MCCodeCampLib {

    private static Plugin plugin;
    private static BlockMarkerRegistry blockMarkerRegistry;
    private static EntityMarkerRegistry entityMarkerRegistry;
    private static ItemsManager itemsManager;

    public static BlockMarkerRegistry getBlockMarkerRegistry() {
        return blockMarkerRegistry;
    }

    public static EntityMarkerRegistry getEntityMarkerRegistry() {
        return entityMarkerRegistry;
    }

    public static ItemsManager getItemsManager() {
        return itemsManager;
    }

    public static void init(@NotNull Plugin plugin) {
        if (MCCodeCampLib.plugin != null) {
            return;
        }
        MCCodeCampLib.plugin = plugin;
        MCCodeCampLib.blockMarkerRegistry = new BlockMarkerRegistry(plugin);
        for (World w : Bukkit.getWorlds()) {
            for (Chunk c : w.getLoadedChunks()) {
                blockMarkerRegistry.onChunkLoad(c);
            }
        }


        MCCodeCampLib.entityMarkerRegistry = new EntityMarkerRegistry(plugin);
        MCCodeCampLib.itemsManager = new ItemsManager(plugin);
        registerEventListeners(plugin);
        PluginHolder.setPlugin((JavaPlugin) plugin);
        InstanceData.setPlugin(plugin);

    }

    private static void registerEventListeners(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new Listeners(), plugin);
        Bukkit.getPluginManager().registerEvents(new ActivationListeners(), plugin);
        Bukkit.getPluginManager().registerEvents(new RegionActivationHandler(), plugin);
        Bukkit.getPluginManager().registerEvents(new EditorListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new me.simoncrafter.CraftersChatDialogs.Listeners(), plugin);
    }

}
