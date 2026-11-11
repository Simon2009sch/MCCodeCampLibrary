package me.simoncrafter.mCCodeCampLibrary.animation;

import me.simoncrafter.mCCodeCampLibrary.animation.schematic.AnimatedSchematicStore.Snapshot;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.IEditable;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.display.EditableDisplay;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.PlayerClickEditableObjectEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.*;
import me.simoncrafter.mCCodeCampLibrary.obstical.AOpenableObject.OpenableRegion;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.joml.Vector3f;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

/** Registry-owned schematic animation. Definitions live in marker JSON, never in display persistence. */
public final class AnimatedBlockObject implements IBlockRegestryObject, IEditable {
    private final boolean proximity;
    private Plugin plugin;
    private Location location;
    private String id;
    private RegistryObjectType type;
    private UUID uuid;
    private ConfigurationNode config;
    private BlockMarkerRegistry registry;
    private AnimationSettings settings;
    private AnimationTrack track;
    private AnimatedBlockRuntime runtime;
    private SchematicScene scene;
    private Snapshot snapshot;
    private EditableDisplay editorDisplay;
    private List<Vector3f> storage=List.of();
    private boolean editMode, destroyed, failed;
    private long tick;

    public AnimatedBlockObject(boolean proximity) { this.proximity=proximity; }

    @Override public void init(Plugin plugin, Location location, String id, RegistryObjectType type,
                               ConfigurationNode config, UUID uuid, BlockMarkerRegistry registry) {
        this.plugin=plugin; this.location=location.clone(); this.id=id; this.type=type;
        this.config=config; this.uuid=uuid; this.registry=registry;
        settings=AnimationSettings.read(config,new Vector3f((float)location.getX(),(float)location.getY(),(float)location.getZ()));
        track=AnimationTrack.read(config);
        editMode=config.node("editMode").getBoolean(false);
        Boolean durableMode=manager().editModeOverride(uuid);
        if(durableMode!=null)editMode=durableMode;
        write("editMode",editMode);
        if (proximity && settings.storage()==null && !settings.regions().isEmpty())
            throw new IllegalArgumentException("proximity_blocks requires a storage cuboid");
        // Validate before registering this instance or spawning editor attachments.
        Bukkit.createBlockData(config.node("replacementBlock").getString("minecraft:air"));
        snapshot=manager().snapshot(uuid);
        if (snapshot!=null && !snapshot.worldId().equals(location.getWorld().getUID()))
            throw new IllegalArgumentException("Saved schematic belongs to another world");
        manager().register(this);
        editorDisplay=new EditableDisplay(location.clone().add(.5,.5,.5),plugin,Component.text(id),event->
                new PlayerClickEditableObjectEvent(event.getPlayer(),this,PlayerClickEditableObjectEvent.ClickType.RIGHT).callEvent());
    }

    private AnimatedObjectManager manager() { return MCCodeCampLib.getAnimatedObjectManager(); }
    public boolean isProximity() { return proximity; }
    public AnimationSettings settings() { return settings; }
    public boolean isEditMode() { return editMode; }
    public boolean hasSchematic() { return snapshot!=null; }
    public Plugin getPlugin() { return plugin; }
    public Map<String,Object> status() {
        return Map.of("editMode",editMode,"saved",snapshot!=null,"failed",failed,
                "runtimeActive",runtime!=null,"displays",scene==null?0:scene.displayCount(),"tick",tick,
                "sourceCells",snapshot==null?0:snapshot.blocks().size(),"transformUpdates",scene==null?0:scene.updateCount());
    }

    /** Runs only from the library's single server-thread tick loop. */
    void tick() {
        if (destroyed || failed || editMode || snapshot==null) return;
        if (runtime==null) createRuntime();
        if (proximity) {
            List<Vector3f> players=location.getWorld().getPlayers().stream()
                    .filter(p->settings.includeSpectators() || p.getGameMode()!=GameMode.SPECTATOR)
                    .map(p->p.getLocation().toVector().toVector3f()).toList();
            runtime.proximity(players,storage,settings.nearDistance(),settings.storageDistance(),settings.activeDistance(),settings.curveHeight());
        } else runtime.timed(track.at(tick++),settings.pivot());
    }

    private void createRuntime() {
        Set<org.bukkit.util.Vector> selected=new HashSet<>();
        for(var region:settings.regions())selected.addAll(region.getBlockVectorList());
        if(snapshot.blocks().size()!=selected.size() || snapshot.blocks().stream().anyMatch(b->
                !selected.contains(new org.bukkit.util.Vector(b.x(),b.y(),b.z()))))
            throw new IllegalArgumentException("Source areas changed: enable edit mode and save the schematic again");
        // Air remains in the schematic for restoration, but needs no entity or replacement.
        var samples=snapshot.blocks().stream().filter(b->!b.blockData().getMaterial().isAir()).toList();
        if (samples.size()>3000) throw new IllegalArgumentException("Schematic exceeds 3000 animated blocks");
        if (proximity) {
            if (settings.storage()==null) throw new IllegalArgumentException("Select a storage area first");
            storage=ProximityMath.scatter(samples.size(),settings.storage().getMin().toVector3f(),
                    settings.storage().getMax().clone().add(new org.bukkit.util.Vector(1,1,1)).toVector3f(),uuid.getLeastSignificantBits());
        }
        var replacement=Bukkit.createBlockData(proximity?"minecraft:air":config.node("replacementBlock").getString("minecraft:air"));
        try { manager().arm(uuid); } catch(IOException e) { throw new UncheckedIOException(e); }
        scene=new SchematicScene(plugin,location.getWorld(),samples,replacement,b->{
            try { snapshot.restoreBlock(location.getWorld(),b.position()); }
            catch(IOException e) { throw new UncheckedIOException(e); }
        });
        runtime=new AnimatedBlockRuntime(scene);
    }

    /** Freezes animation and exposes originals for editing. Leaving edit mode requires a saved snapshot. */
    public void setEditMode(boolean editing) {
        if (destroyed) return;
        if (!editing && snapshot==null) throw new IllegalArgumentException("Save the schematic first");
        try {
            if(editing)manager().beginEditing(uuid);
            else manager().activate(uuid);
        }catch(IOException e){throw new UncheckedIOException(e);}
        if (runtime!=null) { runtime.close(); runtime=null; scene=null; }
        editMode=editing;
        failed=false;
        write("editMode",editing);
        registry.saveObject(this);
    }

    /** Commits a new source schematic. Existing originals are never captured from animation replacements. */
    public void saveSchematic() throws IOException {
        if (destroyed) throw new IllegalStateException("Marker was unloaded");
        if (snapshot!=null && !editMode) throw new IllegalArgumentException("Enable edit mode before recapturing originals");
        if(!editMode)setEditMode(true);
        manager().assertEditable(uuid);
        if (runtime!=null) { runtime.close(); runtime=null; scene=null; }
        snapshot=manager().capture(uuid,location.getWorld(),settings.regions());
        tick=0;
        registry.saveObject(this);
    }

    public void setRegions(Set<OpenableRegion> regions) {
        manager().assertEditable(uuid);
        if (!editMode && snapshot!=null) throw new IllegalArgumentException("Enable edit mode before changing source areas");
        ConfigurationNode updated=config.copy();
        updated.removeChild("blocks");
        for (OpenableRegion region:regions) updated.node("blocks").appendListNode().from(regionConfig(region));
        settings=AnimationSettings.read(updated,settings.pivot());
        config.from(updated);
        registry.saveObject(this);
    }

    public void setPivot(org.bukkit.util.Vector pivot) {
        ConfigurationNode updated=config.copy();
        try { updated.node("pivot","x").set(pivot.getX());updated.node("pivot","y").set(pivot.getY());updated.node("pivot","z").set(pivot.getZ()); }
        catch(SerializationException e) { throw new IllegalArgumentException(e); }
        settings=AnimationSettings.read(updated,settings.pivot());config.from(updated);registry.saveObject(this);
    }

    public void setStorage(OpenableRegion storage) {
        manager().assertEditable(uuid);
        if (!editMode && snapshot!=null) throw new IllegalArgumentException("Enable edit mode before changing storage");
        ConfigurationNode updated=config.copy();updated.node("storage").from(regionConfig(storage));
        settings=AnimationSettings.read(updated,settings.pivot());config.from(updated);registry.saveObject(this);
    }

    private static ConfigurationNode regionConfig(OpenableRegion region) {
        try { return region.getConfig(); } catch(SerializationException e) { throw new IllegalArgumentException(e); }
    }
    private void write(String key,Object value) {
        try { config.node(key).set(value); } catch(SerializationException e) { throw new IllegalArgumentException(e); }
    }

    void suspend() { if(runtime!=null)runtime.suspend(); }
    void fail() { failed=true; }
    boolean touchesChunk(int x,int z) { return scene!=null && scene.touchesChunk(x,z); }
    @Override public void destroy() {
        if(destroyed)return;destroyed=true;
        if(runtime!=null) {
            try {runtime.close();}catch(RuntimeException e){plugin.getLogger().warning("Animation cleanup failed; recovery snapshot retained: "+e.getMessage());}
            runtime=null;scene=null;
        }
        if(editorDisplay!=null){editorDisplay.remove();editorDisplay=null;}
        manager().unregister(this);IBlockRegestryObject.super.destroy();
    }
    @Override public void openEditor(Player player) {
        var manager=MCCodeCampLib.getEditorManager();String key="mccodecamp:animated:"+uuid;
        if(manager.getEditor(key)==null)manager.registerEditor(key,
                new me.simoncrafter.mCCodeCampLibrary.input.editor.AnimatedObjectEditor(plugin,this),this);
        manager.put(player,key);
    }
    @Override public void select(Player player) { if(editorDisplay!=null)editorDisplay.select(player); }
    @Override public void deselect(Player player) { if(editorDisplay!=null)editorDisplay.deselect(player); }
    @Override public void showFor(Player player) { if(editorDisplay!=null)editorDisplay.showFor(player); }
    @Override public void hideFor(Player player) { if(editorDisplay!=null)editorDisplay.hideFor(player); }
    @Override public String getID(){return id;}
    @Override public UUID getUUID(){return uuid;}
    @Override public Location getLocation(){return location.clone();}
    @Override public ConfigurationNode getConfig(){return config;}
    @Override public String getTypeID(){return type.getTypeID();}
    @Override public void setLocation(Location location) { throw new UnsupportedOperationException("Recreate the marker to move its anchor; edit pivot to move the model origin"); }
}
