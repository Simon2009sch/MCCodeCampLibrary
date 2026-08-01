package me.simoncrafter.mCCodeCampLibrary.utility;

import me.simoncrafter.CraftersChatDialogs.InstanceData;
import me.simoncrafter.CraftersDisplayLibrary.PluginHolder;
import me.simoncrafter.CraftersDisplayLibrary.persistence.DisplayPersistence;
import me.simoncrafter.mCCodeCampLibrary.input.activation.ActivationButton;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenue.HotbarMenu;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.BlockMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.RegistryObjectType;
import me.simoncrafter.mCCodeCampLibrary.internal.Listeners;
import me.simoncrafter.mCCodeCampLibrary.internal.activation.ActivationListeners;
import me.simoncrafter.mCCodeCampLibrary.internal.activation.RegionActivationHandler;
import me.simoncrafter.mCCodeCampLibrary.obstical.AOpenableObject;
import me.simoncrafter.mCCodeCampLibrary.obstical.Door.PivotingDoor;
import org.bukkit.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class MCCodeCampLib {

    private static Plugin plugin;
    private static BlockMarkerRegistry blockMarkerRegistry;
    private static ItemsManager itemsManager;
    private static long currentDisplayPluginIteration = 0;

    private static AOpenableObject door;

    public static BlockMarkerRegistry getBlockMarkerRegistry() {
        return blockMarkerRegistry;
    }

    public static Plugin unsafePluginGetDoNotUseThisOutsideOfTesting() {
        return plugin;
    }


    public static long getCurrentDisplayPluginIteration() {
        return currentDisplayPluginIteration;
    }

    public static ItemsManager getItemsManager() {
        return itemsManager;
    }

    public static Logger getPluginLogger() {
        return plugin.getLogger();
    }

    public static void init(@NotNull Plugin plugin) {
        if (MCCodeCampLib.plugin != null) {
            return;
        }
        MCCodeCampLib.plugin = plugin;
        MCCodeCampLib.blockMarkerRegistry = new BlockMarkerRegistry(plugin);
        blockMarkerRegistry.registerObjectType("button", new RegistryObjectType("button", ActivationButton::new));
        for (World w : Bukkit.getWorlds()) {
            for (Chunk c : w.getLoadedChunks()) {
                blockMarkerRegistry.onChunkLoad(c);
            }
        }
        MCCodeCampLib.currentDisplayPluginIteration = System.currentTimeMillis();


        MCCodeCampLib.itemsManager = new ItemsManager(plugin);
        registerEventListeners(plugin);
        PluginHolder.setPlugin((JavaPlugin) plugin);
        InstanceData.setPlugin(plugin);

        //remove all old displays
        for (World w : Bukkit.getWorlds()) {
            DisplayPersistence.removeAllIterations(w);
        }

    }

    public static void onDisable() {
        HotbarMenu.onDisablePlugin();
    }


    public static void toggleDoor() {
        if (!door.isOpen()) {
            door.open();
        } else {
            door.close();
        }
    }

    private static void registerEventListeners(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new Listeners(), plugin);
        Bukkit.getPluginManager().registerEvents(new ActivationListeners(), plugin);
        Bukkit.getPluginManager().registerEvents(new RegionActivationHandler(), plugin);
        Bukkit.getPluginManager().registerEvents(new me.simoncrafter.CraftersChatDialogs.Listeners(), plugin);
    }

    private static Set<Vector> getBlockVectorList(Vector end) {
        Set<Vector> out = new HashSet<>();
        for (float x = 0; x < end.getX(); x++) {
            for (float y = 0; y < end.getY(); y++) {
                for (float z = 0; z < end.getZ(); z++) {
                    out.add(new Vector(x, y, z));
                }
            }
        }
        return out;
    }

}
