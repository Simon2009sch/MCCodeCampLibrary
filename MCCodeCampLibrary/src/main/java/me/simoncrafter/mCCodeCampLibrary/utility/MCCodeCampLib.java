package me.simoncrafter.mCCodeCampLibrary.utility;

import com.sk89q.worldedit.world.registry.SimpleItemMaterial;
import me.simoncrafter.CraftersChatDialogs.InstanceData;
import me.simoncrafter.CraftersDisplayLibrary.PluginHolder;
import me.simoncrafter.CraftersDisplayLibrary.persistence.DisplayPersistence;
import me.simoncrafter.mCCodeCampLibrary.internal.BlockMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.internal.EntityMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.internal.Listeners;
import me.simoncrafter.mCCodeCampLibrary.internal.activation.ActivationListeners;
import me.simoncrafter.mCCodeCampLibrary.internal.activation.RegionActivationHandler;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.EditorListener;
import me.simoncrafter.mCCodeCampLibrary.obstical.AOpenableObject;
import me.simoncrafter.mCCodeCampLibrary.obstical.Door.PivotingDoor;
import me.simoncrafter.mCCodeCampLibrary.obstical.Door.SimpleDoor;
import me.simoncrafter.mCCodeCampLibrary.obstical.Door.SlidingDoor;
import me.simoncrafter.mCCodeCampLibrary.obstical.IOpenable;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MCCodeCampLib {

    private static Plugin plugin;
    private static BlockMarkerRegistry blockMarkerRegistry;
    private static EntityMarkerRegistry entityMarkerRegistry;
    private static ItemsManager itemsManager;
    private static long currentDisplayPluginIteration = 0;

    private static AOpenableObject door;

    public static BlockMarkerRegistry getBlockMarkerRegistry() {
        return blockMarkerRegistry;
    }

    public static EntityMarkerRegistry getEntityMarkerRegistry() {
        return entityMarkerRegistry;
    }

    public static long getCurrentDisplayPluginIteration() {
        return currentDisplayPluginIteration;
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
        MCCodeCampLib.currentDisplayPluginIteration = System.currentTimeMillis();


        MCCodeCampLib.entityMarkerRegistry = new EntityMarkerRegistry(plugin);
        MCCodeCampLib.itemsManager = new ItemsManager(plugin);
        AOpenableObject.setPlugin(plugin);
        registerEventListeners(plugin);
        PluginHolder.setPlugin((JavaPlugin) plugin);
        InstanceData.setPlugin(plugin);

        //remove all old displays
        for (World w : Bukkit.getWorlds()) {
            DisplayPersistence.removeAllIterations(w);
        }


/*
        door = new SimpleDoor("test_door",
                new Location(Bukkit.getWorlds().get(0), -1235, 90, 1141),
                getBlockVectorList(new Vector(1, 4, 3)), Material.BEDROCK, Material.BARRIER);
        */
        /*
        door = new PivotingDoor("test_door",
                new Location(Bukkit.getWorlds().get(0), -1235, 90, 1141),
                getBlockVectorList(new Vector(1, 4, 3)),
                20, new Vector(0.5f, 0, 0.5f), false, 80);
        */
        door = new SlidingDoor("test_door",
                new Location(Bukkit.getWorlds().get(0), -1235, 90, 1141),
                getBlockVectorList(new Vector(1, 4, 3)),
                20, new Vector(0.5f, 0, 0.5f), false, new Vector(0, 0, -3));

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
        Bukkit.getPluginManager().registerEvents(new EditorListener(), plugin);
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
