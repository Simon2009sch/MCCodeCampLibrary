package me.simoncrafter.mCCodeCampLibraryTestPlugin;

import me.simoncrafter.mCCodeCampLibrary.commands.CourseEditCommand;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import me.simoncrafter.mCCodeCampLibraryTestPlugin.testing.ProjectileTestSupport;
import me.simoncrafter.mCCodeCampLibraryTestPlugin.testing.ProjectileEffectsCommand;
import me.simoncrafter.mCCodeCampLibrary.projectile.effect.ProjectileEffects;
import me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.a1_entranceDoor.EntranceDoorCourseSegment;
import me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.a2_bigDoor.BigDoorCourseSegment;
import me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.a3_wand.WandCourseSegment;
import net.kyori.adventure.text.Component;import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public final class MCCodeCampLibraryTestPlugin extends JavaPlugin {

    private ProjectileTestSupport projectileTests;
    private ProjectileEffects projectileEffects;

    @Override
    public void onEnable() {
        MCCodeCampLib.init(this);
        projectileEffects = new ProjectileEffects(this);
        projectileTests = new ProjectileTestSupport(this, MCCodeCampLib.getItemsManager(), projectileEffects);
        java.util.Objects.requireNonNull(getCommand("projectileeffects")).setExecutor(new ProjectileEffectsCommand(projectileEffects));
        Bukkit.getPluginCommand("coursedit").setExecutor((CommandExecutor) new CourseEditCommand(this));
        Bukkit.broadcast(Component.text("Reloaded!!"));
        MCCodeCampLib.getCourseManager().registerSegment(new EntranceDoorCourseSegment(this));
        MCCodeCampLib.getCourseManager().registerSegment(new BigDoorCourseSegment(this));
        MCCodeCampLib.getCourseManager().registerSegment(new WandCourseSegment(this));


    }

    @Override
    public void onDisable() {
        if (projectileTests != null) {
            projectileTests.close();
            projectileTests = null;
        }
        if (projectileEffects != null) {
            projectileEffects.close();
            projectileEffects = null;
        }
        MCCodeCampLib.onDisable();
    }
}