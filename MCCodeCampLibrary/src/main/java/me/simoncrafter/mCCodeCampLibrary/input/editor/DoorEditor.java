package me.simoncrafter.mCCodeCampLibrary.input.editor;

import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.GenericQuestion;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.area.PivotEditor;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.area.CuboidAreaEditor;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.area.CuboidAreaSelection;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenu.HotbarMenu;
import me.simoncrafter.mCCodeCampLibrary.obstical.AOpenableObject;
import me.simoncrafter.mCCodeCampLibrary.obstical.AAnimatedOpenableObject;
import me.simoncrafter.mCCodeCampLibrary.obstical.AOpenableObject.OpenableRegion;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import java.util.*;

/** Door area and pivot drafts; Save persists, Back discards. Pivot coordinates are marker-relative. */
public final class DoorEditor extends CuboidAreaEditor {
    private final AOpenableObject door;
    private final Map<UUID, Vector> pivots = new HashMap<>();

    /** Creates an owner-scoped editor for an openable marker. */
    public DoorEditor(Plugin plugin, AOpenableObject door) {
        super(plugin, door.getLocation().getWorld(),
                ()->CuboidAreaSelection.translate(door.getBlockRegions(),door.getLocation().toVector()),
                (player, areas)->door.setBlockRegions(new LinkedHashSet<>(CuboidAreaSelection.translate(
                        areas,door.getLocation().toVector().multiply(-1)))));
        this.door = door;
    }

    @Override protected void join(Player player) {
        super.join(player);
        if (door instanceof AAnimatedOpenableObject animated) {
            pivots.put(player.getUniqueId(), animated.getPivotPosition());
        }
    }

    @Override protected void leave(Player player) {
        pivots.remove(player.getUniqueId());
        super.leave(player);
    }

    @Override protected void configureHome(Player player, GenericQuestion question, HotbarMenu menu) {
        if (door instanceof AAnimatedOpenableObject) {
            addControl(question, menu, 4, Material.COMPASS, "Edit pivot (relative to marker)", this::showPivot);
        }
    }

    @Override protected void save(Player player, List<OpenableRegion> areas) {
        if (door instanceof AAnimatedOpenableObject animated) {
            animated.setPivotPosition(pivots.get(player.getUniqueId()));
        }
        super.save(player, areas);
    }

    private void showPivot(Player player) {
        if (!active(player)) return;
        var manager=MCCodeCampLib.getEditorManager();
        String key="mccodecamp:door-pivot:"+door.getUUID();
        if (manager.getEditor(key)==null) manager.registerEditor(key,new PivotEditor(getPlugin(),door.getLocation(),
                p->pivots.get(p.getUniqueId()),(p,value)->pivots.put(p.getUniqueId(),value)),door);
        manager.put(player,key);
    }
}
