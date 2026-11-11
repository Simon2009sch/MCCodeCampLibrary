package me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.a4_flightFeather;

import me.simoncrafter.mCCodeCampLibrary.courseapi.ACourseSegment;
import me.simoncrafter.mCCodeCampLibrary.input.activation.event.playerBlockActivation.ButtonIDEvent;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import me.simoncrafter.mCCodeCampLibraryTestPlugin.yourCode.a4_flightFeather.Course;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/** Owns the student's item registration and every temporary wind display. */
public final class FlightFeatherCourseSegment extends ACourseSegment {
    private Course student;
    private WindEffect wind;

    public FlightFeatherCourseSegment(Plugin plugin) { super(plugin); }

    @Override public String getSegmentID() { return "flightFeather"; }

    @EventHandler public void onButtonID(ButtonIDEvent event) {
        if (student == null || !"get_flight_feather".equals(event.getID())) return;
        ItemStack item = MCCodeCampLib.getItemsManager().createNewItem(student);
        if (item != null) {
            // Do not silently discard the item when a student's inventory is full.
            event.getPlayer().getInventory().addItem(item).values().forEach(leftover ->
                    event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), leftover));
        }
    }

    @EventHandler(ignoreCancelled = true) public void onSneak(PlayerToggleSneakEvent event) {
        if (student != null) student.onSneak(event);
    }

    @Override protected void onTick() {
        if (wind != null) wind.tick();
    }

    @Override protected void onActivate() {
        if (student != null) return;
        wind = new WindEffect();
        student = new Course(wind::burst);
        MCCodeCampLib.getItemsManager().registerCustomItem(student);
    }

    @Override protected void onDeactivate() {
        if (student != null) {
            student.close();
            MCCodeCampLib.getItemsManager().unregisterCustomItem(student);
            student = null;
        }
        if (wind != null) {
            wind.close();
            wind = null;
        }
    }
}
