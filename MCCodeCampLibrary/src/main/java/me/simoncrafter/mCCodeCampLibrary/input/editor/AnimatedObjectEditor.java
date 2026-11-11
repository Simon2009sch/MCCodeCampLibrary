package me.simoncrafter.mCCodeCampLibrary.input.editor;

import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.GenericQuestion;
import me.simoncrafter.mCCodeCampLibrary.animation.AnimatedBlockObject;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.area.CuboidAreaEditor;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenu.HotbarMenu;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashSet;
import java.util.List;

/** Minimal source/storage authoring; keyframes remain plain world-marker JSON. */
public final class AnimatedObjectEditor extends CuboidAreaEditor {
    private final AnimatedBlockObject object;

    public AnimatedObjectEditor(Plugin plugin,AnimatedBlockObject object) {
        super(plugin,object.getLocation().getWorld(),()->object.settings().regions(),
                (player,areas)->object.setRegions(new LinkedHashSet<>(areas)));
        this.object=object;
    }

    @Override protected void configureHome(Player player,GenericQuestion question,HotbarMenu menu) {
        addControl(question,menu,4,Material.LEVER,object.isEditMode()?"Finish editing / animate":"Edit originals as real blocks",p->{
            object.setEditMode(!object.isEditMode());showHome(p);
        });
        if(object.isProximity())addControl(question,menu,5,Material.CHEST,"Select storage area",this::storage);
        else addControl(question,menu,5,Material.COMPASS,"Edit pivot",this::pivot);
        addControl(question,menu,6,Material.WRITABLE_BOOK,"Save source schematic",p->{
            object.setRegions(new LinkedHashSet<>(draft(p).getAreas()));
            try {object.saveSchematic();}catch(IOException e){throw new UncheckedIOException(e);}
            p.sendMessage(Component.text("Schematic committed. Finish editing to animate."));showHome(p);
        });
    }

    private void storage(Player player) {
        var manager=MCCodeCampLib.getEditorManager();
        String key="mccodecamp:animated-storage:"+object.getUUID();
        if(manager.getEditor(key)==null)manager.registerEditor(key,new CuboidAreaEditor(getPlugin(),object.getLocation().getWorld(),
                ()->object.settings().storage()==null?List.of():List.of(object.settings().storage()),(p,areas)->{
                    if(areas.size()!=1)throw new IllegalArgumentException("Storage must be exactly one cuboid");
                    object.setStorage(areas.getFirst());
                }),object);
        manager.put(player,key);
    }

    private void pivot(Player player) {
        var manager=MCCodeCampLib.getEditorManager();String key="mccodecamp:animated-pivot:"+object.getUUID();
        if(manager.getEditor(key)==null)manager.registerEditor(key,
                new me.simoncrafter.mCCodeCampLibrary.internal.editor.area.PivotEditor(getPlugin(),object.getLocation(),
                        p->new org.bukkit.util.Vector(object.settings().pivot().x,object.settings().pivot().y,object.settings().pivot().z)
                                .subtract(object.getLocation().toVector()),
                        (p,value)->object.setPivot(value.add(object.getLocation().toVector()))),object);
        manager.put(player,key);
    }
}
