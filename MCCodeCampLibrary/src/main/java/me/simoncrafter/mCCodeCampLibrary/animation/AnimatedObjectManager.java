package me.simoncrafter.mCCodeCampLibrary.animation;

import me.simoncrafter.mCCodeCampLibrary.animation.schematic.AnimatedSchematicStore;
import me.simoncrafter.mCCodeCampLibrary.animation.schematic.AnimatedSchematicStore.Snapshot;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.events.BlockRegistryUpdateEvent;
import me.simoncrafter.mCCodeCampLibrary.obstical.AOpenableObject.OpenableRegion;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.*;
import org.bukkit.plugin.Plugin;
import java.io.*;
import java.util.*;
import static me.simoncrafter.mCCodeCampLibrary.animation.AnimationRecoveryJournal.State.*;

/** Plugin-owned ticks, exact-cell ownership and crash recovery, independent of marker-anchor chunks. */
public final class AnimatedObjectManager implements Listener, AutoCloseable {
    private final Plugin plugin;
    private final AnimatedSchematicStore store;
    private final AnimationRecoveryJournal journal;
    private final AnimationSourceClaims claims=new AnimationSourceClaims();
    private final Map<UUID,Snapshot> snapshots=new HashMap<>();
    private final Map<UUID,AnimatedBlockObject> objects=new HashMap<>();
    private final Map<UUID,AnimationRecoveryJournal.Entry> intents=new HashMap<>();
    private boolean closed;

    public AnimatedObjectManager(Plugin plugin)throws IOException {
        this.plugin=plugin;
        requireWorldEdit();
        store=new AnimatedSchematicStore(plugin);
        journal=new AnimationRecoveryJournal(plugin.getDataFolder().toPath().resolve("animated"));
        // Decode and validate ALL originals before permitting any startup world mutations.
        for(UUID id:store.objectIds()) {
            Snapshot snapshot=store.load(id);snapshots.put(id,snapshot);
            var intent=journal.read(id);
            if(intent==null)intent=new AnimationRecoveryJournal.Entry(EDITING,Set.of());
            if(intent.state()==RESTORING && intent.pending().isEmpty())intent=new AnimationRecoveryJournal.Entry(RESTORING,chunks(snapshot));
            intents.put(id,intent);
            if(intent.state()!=DELETED) {
                try {claims.replace(id,snapshot.worldId(),cells(snapshot));}
                catch(IllegalArgumentException e){throw new IOException("Overlapping saved animation sources",e);}
            }
        }
    }

    private static void requireWorldEdit()throws IOException {
        var plugins=Bukkit.getPluginManager();
        Plugin fawe=plugins.getPlugin("FastAsyncWorldEdit"),we=plugins.getPlugin("WorldEdit");
        if((fawe==null||!fawe.isEnabled())&&(we==null||!we.isEnabled()))
            throw new IOException("Animations require enabled WorldEdit or FastAsyncWorldEdit before initialization");
        try {
            com.sk89q.worldedit.WorldEdit.getInstance().getPlatformManager()
                    .queryCapability(com.sk89q.worldedit.extension.platform.Capability.WORLD_EDITING);
        }catch(RuntimeException|LinkageError e){throw new IOException("WorldEdit editing platform is not ready",e);}
    }

    public Snapshot snapshot(UUID id){return snapshots.get(id);}
    void register(AnimatedBlockObject object){objects.put(object.getUUID(),object);}
    void unregister(AnimatedBlockObject object){objects.remove(object.getUUID(),object);}

    /** Durable edit intent takes precedence over stale chunk JSON after a crash. */
    Boolean editModeOverride(UUID id) {
        var intent=intents.get(id);
        if(intent==null) {
            try {intent=journal.read(id);}catch(IOException e){throw new UncheckedIOException(e);}
        }
        return intent==null?null:intent.state()!=ACTIVE;
    }

    public Snapshot capture(UUID id,World world,Collection<OpenableRegion> regions)throws IOException {
        Set<AnimationSourceClaims.Cell> selected=new HashSet<>();
        for(var region:regions) {
            var a=region.getMin();var b=region.getMax();
            long x=(long)b.getBlockX()-a.getBlockX()+1,y=(long)b.getBlockY()-a.getBlockY()+1,z=(long)b.getBlockZ()-a.getBlockZ()+1;
            if(x<=0||y<=0||z<=0||x>250000||y>250000||z>250000||x*y>250000||x*y*z>250000)
                throw new IllegalArgumentException("Source area too large");
            region.doForEveryBlock(v->{selected.add(new AnimationSourceClaims.Cell(v.getBlockX(),v.getBlockY(),v.getBlockZ()));
                if(selected.size()>250000)throw new IllegalArgumentException("Source selection too large");});
        }
        claims.check(id,world.getUID(),selected);
        assertEditable(id);
        Snapshot snapshot=store.exists(id)?store.recapture(id,world,regions):store.capture(id,world,regions);
        snapshots.put(id,snapshot);claims.replace(id,world.getUID(),selected);
        setIntent(id,EDITING,Set.of());
        return snapshot;
    }

    /** Records restore-to-edit intent before restoration; caller must not expose editing controls earlier. */
    void beginEditing(UUID id)throws IOException {
        var current=intents.get(id);
        if(current!=null&&current.state()==EDITING)return; // Never overwrite unsaved edits on repeated entry.
        Snapshot snapshot=snapshots.get(id);
        if(snapshot==null){setIntent(id,EDITING,Set.of());return;}
        World world=Bukkit.getWorld(snapshot.worldId());
        if(world==null)throw new IllegalStateException("Source world is unavailable");
        for(var b:snapshot.blocks())if(!world.isChunkLoaded(b.x()>>4,b.z()>>4))
            throw new IllegalStateException("Load all source chunks before editing");
        setIntent(id,RESTORING,chunks(snapshot));
        snapshot.restore(world,p->true);
        setIntent(id,EDITING,Set.of());
    }

    void assertEditable(UUID id) {
        var entry=intents.get(id);
        if(entry!=null && (entry.state()==ACTIVE||entry.state()==RESTORING||entry.state()==RETIRED))
            throw new IllegalStateException("Finish restoring originals / enable edit mode before saving");
    }

    /** Explicit finish transition, durable even when chunk PDC has not reached disk yet. */
    void activate(UUID id)throws IOException {
        Snapshot snapshot=snapshots.get(id);
        if(snapshot==null)throw new IllegalArgumentException("Save the source schematic first");
        var entry=intents.get(id);
        if(entry!=null&&(entry.state()==RESTORING||entry.state()==RETIRED))
            throw new IllegalStateException("Source recovery is still pending");
        claims.replace(id,snapshot.worldId(),cells(snapshot));
        setIntent(id,ACTIVE,Set.of());
    }

    /** Runtime must never silently resume an editing object from stale JSON. */
    void arm(UUID id)throws IOException {
        var entry=intents.get(id);
        if(entry==null||entry.state()!=ACTIVE)throw new IllegalStateException("Explicitly finish edit mode before animation");
        var snapshot=snapshots.get(id);claims.check(id,snapshot.worldId(),cells(snapshot));
    }
    void disarm(UUID id)throws IOException {setIntent(id,EDITING,Set.of());}

    public void restoreLoaded(Snapshot snapshot) {
        World world=Bukkit.getWorld(snapshot.worldId());if(world==null)return;
        try{snapshot.restore(world,p->world.isChunkLoaded(p.x()>>4,p.z()>>4));}
        catch(IOException e){throw new UncheckedIOException(e);}
    }
    /** Called before marker reconstruction; only durable recovery intents permit pasting. */
    public void recoverLoaded(){for(World w:Bukkit.getWorlds())for(Chunk c:w.getLoadedChunks())recover(c);}

    public void tick() {
        if(closed)return;
        for(var object:List.copyOf(objects.values()))try{object.tick();}
        catch(Exception e){
            try{object.suspend();}catch(RuntimeException cleanup){e.addSuppressed(cleanup);}
            object.fail();warn("Animation stopped: "+object.getID(),e);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onChunkLoad(ChunkLoadEvent event){recover(event.getChunk());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        var c=event.getChunk();
        for(var object:List.copyOf(objects.values()))if(object.getLocation().getWorld().equals(c.getWorld())&&object.touchesChunk(c.getX(),c.getZ()))
            suspend(object);
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event){cleanEntities(event.getEntities());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onWorldUnload(WorldUnloadEvent event) {
        for(var object:List.copyOf(objects.values()))if(object.getLocation().getWorld().equals(event.getWorld()))suspend(object);
    }
    private void suspend(AnimatedBlockObject object) {
        try{object.suspend();}catch(RuntimeException e){object.fail();warn("Animation cleanup failed",e);}
    }

    @EventHandler public void onRegistryUpdate(BlockRegistryUpdateEvent event) {
        if(event.getUpdateType()!=BlockRegistryUpdateEvent.UpdateType.DELETE||!(event.getRegistryObject() instanceof AnimatedBlockObject object))return;
        UUID id=object.getUUID();var snapshot=snapshots.get(id);
        try {
            var intent=intents.get(id);
            if(snapshot==null||intent==null||intent.state()==EDITING||intent.state()==DELETED){setIntent(id,DELETED,Set.of());return;}
            World world=object.getLocation().getWorld();restoreLoaded(snapshot);
            Set<Long> pending=new HashSet<>();
            for(var b:snapshot.blocks())if(!world.isChunkLoaded(b.x()>>4,b.z()>>4))pending.add(chunkKey(b.x()>>4,b.z()>>4));
            setIntent(id,pending.isEmpty()?DELETED:RETIRED,pending);
        }catch(IOException|RuntimeException e){warn("Recovery retained for deleted animation "+id,e);}
    }

    private void recover(Chunk chunk) {
        cleanEntities(Arrays.asList(chunk.getEntities()));long key=chunkKey(chunk.getX(),chunk.getZ());
        for(var snapshot:List.copyOf(snapshots.values())) {
            UUID id=snapshot.objectId();var entry=intents.get(id);
            if(!snapshot.worldId().equals(chunk.getWorld().getUID())||entry==null)continue;
            if(entry.state()!=ACTIVE && ((entry.state()!=RETIRED&&entry.state()!=RESTORING)||!entry.pending().contains(key)))continue;
            try {
                snapshot.restore(chunk.getWorld(),p->(p.x()>>4)==chunk.getX()&&(p.z()>>4)==chunk.getZ());
                if(entry.state()!=ACTIVE) {
                    Set<Long> pending=new HashSet<>(entry.pending());pending.remove(key);
                    setIntent(id,pending.isEmpty()?(entry.state()==RESTORING?EDITING:DELETED):entry.state(),pending);
                }
            }catch(Exception e){warn("Chunk recovery failed for "+id,e);}
        }
    }

    // Exposed originals are authoritative scenery, not mutable inventories/terrain. Edit mode permits changes.
    private boolean protectedSource(Location location) {
        if(location==null)return false;
        for(var snapshot:snapshots.values()) {
            var intent=intents.get(snapshot.objectId());
            if(intent==null||(intent.state()!=ACTIVE&&intent.state()!=RESTORING)||!snapshot.worldId().equals(location.getWorld().getUID()))continue;
            for(var p:snapshot.blocks())if(p.x()==location.getBlockX()&&p.y()==location.getBlockY()&&p.z()==location.getBlockZ())return true;
        }
        return false;
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onBreak(BlockBreakEvent event){if(protectedSource(event.getBlock().getLocation()))event.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onPlace(BlockPlaceEvent event){if(protectedSource(event.getBlock().getLocation()))event.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onInteract(PlayerInteractEvent event){if(event.getClickedBlock()!=null&&protectedSource(event.getClickedBlock().getLocation()))event.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onInventoryOpen(InventoryOpenEvent event){if(protectedSource(event.getInventory().getLocation()))event.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void onInventoryMove(InventoryMoveItemEvent event){if(protectedSource(event.getSource().getLocation())||protectedSource(event.getDestination().getLocation()))event.setCancelled(true);}

    private void setIntent(UUID id,AnimationRecoveryJournal.State state,Set<Long> pending)throws IOException {
        var entry=new AnimationRecoveryJournal.Entry(state,pending);journal.write(id,entry);intents.put(id,entry);
        if(state==DELETED)claims.release(id);
    }
    private static Set<AnimationSourceClaims.Cell> cells(Snapshot snapshot) {
        Set<AnimationSourceClaims.Cell> cells=new HashSet<>();for(var p:snapshot.blocks())cells.add(new AnimationSourceClaims.Cell(p.x(),p.y(),p.z()));return cells;
    }
    private static Set<Long> chunks(Snapshot snapshot) {
        Set<Long> chunks=new HashSet<>();for(var p:snapshot.blocks())chunks.add(chunkKey(p.x()>>4,p.z()>>4));return chunks;
    }
    private static long chunkKey(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    private static void cleanEntities(Collection<Entity> entities) {
        for(var entity:entities)if(entity.getScoreboardTags().contains(SchematicScene.ENTITY_TAG)
                &&!entity.getScoreboardTags().contains(SchematicScene.SESSION_TAG))entity.remove();
    }
    private void warn(String message,Exception e){plugin.getLogger().warning(message+": "+e.getMessage());}
    @Override public void close() {
        if(closed)return;closed=true;
        for(var object:List.copyOf(objects.values()))try{object.destroy();}catch(RuntimeException e){warn("Animation shutdown failed",e);}
        objects.clear();HandlerList.unregisterAll(this);
    }
}
