package me.simoncrafter.mCCodeCampLibrary.utility;

import me.simoncrafter.CraftersChatDialogs.InstanceData;
import me.simoncrafter.CraftersDisplayLibrary.PluginHolder;
import me.simoncrafter.CraftersDisplayLibrary.persistence.DisplayPersistence;
import me.simoncrafter.mCCodeCampLibrary.courseapi.CourseManager;
import me.simoncrafter.mCCodeCampLibrary.input.activation.ActivationButton;
import me.simoncrafter.mCCodeCampLibrary.input.activation.ProjectileTarget;
import me.simoncrafter.mCCodeCampLibrary.input.editor.WorldMarkerEditor;
import me.simoncrafter.mCCodeCampLibrary.input.item.ItemsManager;
import me.simoncrafter.mCCodeCampLibrary.projectile.ProjectileManager;
import me.simoncrafter.mCCodeCampLibrary.internal.activation.StyledRegistryObjectType;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.EditorManager;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenu.HotbarMenu;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.BlockMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.internal.Listeners;
import me.simoncrafter.mCCodeCampLibrary.internal.activation.ActivationListeners;
import me.simoncrafter.mCCodeCampLibrary.internal.activation.RegionActivationHandler;
import me.simoncrafter.mCCodeCampLibrary.obstical.AOpenableObject;
import me.simoncrafter.mCCodeCampLibrary.obstical.Door.SlidingDoor;
import org.bukkit.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class MCCodeCampLib {

    private static Plugin plugin;
    private static boolean shutDown;
    private static BlockMarkerRegistry blockMarkerRegistry;
    private static EditorManager editorManager;
    private static long currentDisplayPluginIteration = 0;
    private static BukkitRunnable globalTickLoop;
    private static CourseManager courseManager;
    private static ItemsManager itemsManager;
    private static ProjectileManager projectileManager;

    public static ProjectileManager getProjectileManager() {
        if (projectileManager == null) throw new IllegalStateException("MCCodeCampLib is not initialized");
        return projectileManager;
    }

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


    public static Logger getPluginLogger() {
        return plugin.getLogger();
    }

    public static EditorManager getEditorManager() {
        return editorManager;
    }

    public static CourseManager getCourseManager() {
        return courseManager;
    }

    /** Initialize once per plugin classloader; shutdown requires a fresh classloader before reuse. */
    public static void init(@NotNull Plugin plugin) {
        if (shutDown) throw new IllegalStateException("MCCodeCampLib shutdown requires a fresh plugin classloader before initialization");
        if (MCCodeCampLib.plugin != null) {
            return;
        }
        MCCodeCampLib.plugin = plugin;
        ItemCooldown.resetSession();
        projectileManager = new ProjectileManager(plugin);
        MCCodeCampLib.blockMarkerRegistry = new BlockMarkerRegistry(plugin);

        registerWorldMarkerTypes();

        for (World w : Bukkit.getWorlds()) {
            for (Chunk c : w.getLoadedChunks()) {
                blockMarkerRegistry.onChunkLoad(c);
            }
        }
        MCCodeCampLib.currentDisplayPluginIteration = System.currentTimeMillis();
        itemsManager = new ItemsManager(plugin);


        MCCodeCampLib.editorManager = new EditorManager(plugin);
        if (!editorManager.registerEditor("mccodecamp:world-marker", new WorldMarkerEditor(plugin))) {
            throw new IllegalStateException("Could not register the world-marker editor");
        }
        registerEventListeners(plugin);
        PluginHolder.setPlugin((JavaPlugin) plugin);
        InstanceData.setPlugin(plugin);
        HotbarMenu.onPluginLoad(plugin);
        MCCodeCampLib.courseManager = new CourseManager(plugin);


        //remove all old displays
        for (World w : Bukkit.getWorlds()) {
            DisplayPersistence.removeAllIterations(w);
        }

        globalTickLoop = new BukkitRunnable() {
            @Override
            public void run() {
                Cooldown.tickAll();
                ItemCooldown.tick();
                projectileManager.tick();
                courseManager.onTick();
            }
        };
        globalTickLoop.runTaskTimer(plugin, 1L, 1L);



    }

    private static void registerWorldMarkerTypes() {
        blockMarkerRegistry.registerObjectType("projectile_target", new StyledRegistryObjectType(ProjectileTarget::new, "projectile_target", Map.of(
                "displayname", "Projectile Target",
                "description", "A block activated by custom projectiles (optionally vanilla projectiles)",
                "displayItem", Material.TARGET.toString()
        )));
        StyledRegistryObjectType buttonType = new StyledRegistryObjectType(ActivationButton::new, "button", Map.ofEntries(
                Map.entry("displayname", "Button"),
                Map.entry("description", "Creates a new button usable in code"),
                Map.entry("displayItem", Material.OAK_BUTTON.toString())
        ));
        StyledRegistryObjectType slidingDoorType = new StyledRegistryObjectType(SlidingDoor::new, "sliding_door", Map.ofEntries(
                Map.entry("displayname", "Sliding Door"),
                Map.entry("description", "Creates a door that slides open"),
                Map.entry("displayItem", Material.OAK_DOOR.toString())
        ));
        StyledRegistryObjectType pivotingDoor = new StyledRegistryObjectType(SlidingDoor::new, "pivoting_door", Map.ofEntries(
                Map.entry("displayname", "Pivoting Door"),
                Map.entry("description", "Creates a door that works like you would expect a real life door to open"),
                Map.entry("displayItem", Material.DARK_OAK_DOOR.toString())
        ));
        buttonType.setAllowMultipleOfSameID(true);
        blockMarkerRegistry.registerObjectType(
                "button", buttonType
        );
        blockMarkerRegistry.registerObjectType(
                "sliding_door", slidingDoorType
        );
        blockMarkerRegistry.registerObjectType(
                "pivoting_door", pivotingDoor
        );
    }

    public static void onDisable() {
        shutDown = true;
        if (globalTickLoop != null) {
            globalTickLoop.cancel();
            globalTickLoop = null;
        }
        if (projectileManager != null) {
            projectileManager.close();
            projectileManager = null;
        }
        if (blockMarkerRegistry != null) {
            // Loaded targets/buttons own listeners, cooldowns and editor displays.
            for (var object : blockMarkerRegistry.getRegisteredObjects()) object.destroy();
        }
        if (itemsManager != null) {
            itemsManager.close();
            itemsManager = null;
        }
        Cooldown.disposeAll();
        ItemCooldown.resetSession();
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
        Bukkit.getPluginManager().registerEvents(blockMarkerRegistry, plugin);
        Bukkit.getPluginManager().registerEvents(projectileManager, plugin);
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

    public static ItemsManager getItemsManager() {
        return itemsManager;
    }
}
