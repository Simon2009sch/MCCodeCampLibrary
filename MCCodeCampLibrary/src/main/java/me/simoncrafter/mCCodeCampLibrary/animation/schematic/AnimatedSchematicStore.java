package me.simoncrafter.mCCodeCampLibrary.animation.schematic;

import com.sk89q.worldedit.math.BlockVector3;
import me.simoncrafter.mCCodeCampLibrary.obstical.AOpenableObject.OpenableRegion;
import java.util.*;
import java.util.function.Predicate;
import java.io.*;
import java.nio.file.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BaseBlock;

/** Durable snapshots of exact selected cells, independent of display persistence. */
public final class AnimatedSchematicStore {
    static final int LIMIT=250_000;
    private final SchematicFiles files;
    private final Path directory;

    public AnimatedSchematicStore(Plugin plugin) throws IOException {
        requireMain(); directory=Objects.requireNonNull(plugin).getDataFolder().toPath().resolve("animated");
        files=new SchematicFiles(directory);
    }
    /** Absolute world cell; suitable for per-chunk recovery predicates. */
    public record BlockPos(int x,int y,int z) {}
    /** Immutable block sample; every BlockData accessor returns a fresh copy. */
    public record BlockSample(int x,int y,int z,String blockDataString) {
        public BlockData blockData(){return Bukkit.createBlockData(blockDataString);}
        public BlockData data(){return blockData();}
        public BlockPos position(){return new BlockPos(x,y,z);}
    }
    /** Original selected blocks, including selected air and tile NBT. */
    public static final class Snapshot {
        private final UUID objectId,worldId;
        private final Map<BlockVector3,BaseBlock> originals;
        private final List<BlockSample> blocks;
        private Snapshot(UUID objectId,UUID worldId,Map<BlockVector3,BaseBlock> originals) {
            this.objectId=objectId;this.worldId=worldId;this.originals=Collections.unmodifiableMap(new LinkedHashMap<>(originals));
            blocks=originals.entrySet().stream().map(e->new BlockSample(e.getKey().getBlockX(),e.getKey().getBlockY(),e.getKey().getBlockZ(),e.getValue().toImmutableState().getAsString())).toList();
        }
        /** Restores one selected cell in O(1), including tile NBT; never loads its chunk. */
        public void restoreBlock(World world,BlockPos position)throws IOException {
            requireMain();Objects.requireNonNull(world);Objects.requireNonNull(position);
            if(!world.getUID().equals(worldId))throw new IllegalArgumentException("Snapshot belongs to another world");
            var key=BlockVector3.at(position.x(),position.y(),position.z());var block=originals.get(key);
            if(block==null)throw new IllegalArgumentException("Cell is not selected");
            if(position.y()<world.getMinHeight()||position.y()>=world.getMaxHeight())throw new IllegalArgumentException("Cell outside world height");
            if(!world.isChunkLoaded(position.x()>>4,position.z()>>4))throw new IllegalStateException("Selected chunk not loaded");
            try(var session=com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
                session.setFastMode(false);session.setBlock(key,block);
            }catch(com.sk89q.worldedit.WorldEditException e){throw new IOException("Block restore failed; snapshot retained",e);}
            BlockRestoreUpdates.publish(world,Map.of(position,BukkitAdapter.adapt(block.toImmutableState())));
        }
        public UUID objectId(){return objectId;}
        public UUID worldId(){return worldId;}
        public List<BlockSample> blocks(){return blocks;}
        public List<BlockSample> entries(){return blocks;}
        /** Restores exactly filtered cells including tile NBT. Unloaded cells fail before any write.
         * Caller may filter by chunk and retry independently of the anchor chunk. */
        public void restore(World world,Predicate<BlockPos> filter) throws IOException {
            requireMain(); Objects.requireNonNull(filter); validateWorld(this,world);
            var chosen=originals.entrySet().stream().filter(e->filter.test(new BlockPos(e.getKey().getBlockX(),e.getKey().getBlockY(),e.getKey().getBlockZ()))).toList();
            requireLoaded(world,chosen.stream().map(Map.Entry::getKey).toList());
            if(chosen.isEmpty())return;
            var adapted=BukkitAdapter.adapt(world);
            try(var session=com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(adapted)) {
                session.setFastMode(false);
                for(var entry:chosen)session.setBlock(entry.getKey(),entry.getValue());
            }catch(com.sk89q.worldedit.WorldEditException e){throw new IOException("Block restore failed; snapshot retained",e);}
            Map<BlockPos,BlockData> updates=new LinkedHashMap<>();
            for(var entry:chosen) {
                var p=entry.getKey();updates.put(new BlockPos(p.getBlockX(),p.getBlockY(),p.getBlockZ()),
                        BukkitAdapter.adapt(entry.getValue().toImmutableState()));
            }
            BlockRestoreUpdates.publish(world,updates);
        }
    }
    /** Captures the exact union; durable commit completes before returning. Never overwrites originals. */
    public Snapshot capture(UUID id,World world,Collection<OpenableRegion> regions) throws IOException {
        return capture(id,world,regions,false);
    }
    /** Explicit edit-mode recapture; previous committed generations remain recoverable. */
    public Snapshot recapture(UUID id,World world,Collection<OpenableRegion> regions)throws IOException {
        return capture(id,world,regions,true);
    }
    private Snapshot capture(UUID id,World world,Collection<OpenableRegion> regions,boolean replacing)throws IOException {
        requireMain();Objects.requireNonNull(id);Objects.requireNonNull(world);
        var positions=select(regions);validateHeight(world,positions);requireLoaded(world,positions);
        var min=BlockVector3.at(positions.stream().mapToInt(BlockVector3::getBlockX).min().orElseThrow(),positions.stream().mapToInt(BlockVector3::getBlockY).min().orElseThrow(),positions.stream().mapToInt(BlockVector3::getBlockZ).min().orElseThrow());
        var max=BlockVector3.at(positions.stream().mapToInt(BlockVector3::getBlockX).max().orElseThrow(),positions.stream().mapToInt(BlockVector3::getBlockY).max().orElseThrow(),positions.stream().mapToInt(BlockVector3::getBlockZ).max().orElseThrow());
        checkVolume(min,max);
        Map<BlockVector3,BaseBlock> originals=new LinkedHashMap<>();var adapted=BukkitAdapter.adapt(world);
        var clipboard=new BlockArrayClipboard(new CuboidRegion(min,max));clipboard.setOrigin(min);
        try {
            for(var pos:positions) {var block=adapted.getFullBlock(pos);originals.put(pos,block);clipboard.setBlock(pos,block);}
            if(clipboard instanceof Flushable f)f.flush();
            byte[] bytes;
            try(var out=new ByteArrayOutputStream()) {try(var writer=BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(out)){writer.write(clipboard);}bytes=out.toByteArray();}
            var snapshot=new Snapshot(id,world.getUID(),originals);
            if(replacing)files.replace(id,bytes,metadata(snapshot));else files.commit(id,bytes,metadata(snapshot));
            return snapshot;
        } catch(com.sk89q.worldedit.WorldEditException e){throw new IOException("Capture failed",e);}
        finally {closeClipboard(clipboard);}
    }
    /** Validates both files and reconstructs immutable samples; does not inspect/load world chunks. */
    public Snapshot load(UUID id) throws IOException {
        requireMain();var payload=files.read(Objects.requireNonNull(id));
        UUID worldId;Set<BlockVector3> selected=new LinkedHashSet<>();
        try(var in=new DataInputStream(new ByteArrayInputStream(payload.metadata()))) {
            if(in.readInt()!=1||!id.equals(new UUID(in.readLong(),in.readLong())))throw new IOException("Invalid object metadata");
            worldId=new UUID(in.readLong(),in.readLong());int count=in.readInt();
            if(count<=0||count>LIMIT)throw new IOException("Invalid selection count");
            for(int i=0;i<count;i++)if(!selected.add(BlockVector3.at(in.readInt(),in.readInt(),in.readInt())))throw new IOException("Duplicate selection cell");
            if(in.read()!=-1)throw new IOException("Trailing selection data");
        }
        try(var input=new ByteArrayInputStream(payload.schematic());var reader=BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(input)) {
            var clipboard=reader.read();
            try {
                checkVolume(clipboard.getRegion().getMinimumPoint(),clipboard.getRegion().getMaximumPoint());
                var base=clipboard.getRegion().getMinimumPoint();
                // Sponge readers may rebase coordinates to zero; original absolute positions are in the manifest.
                var min=BlockVector3.at(selected.stream().mapToInt(BlockVector3::getBlockX).min().orElseThrow(),selected.stream().mapToInt(BlockVector3::getBlockY).min().orElseThrow(),selected.stream().mapToInt(BlockVector3::getBlockZ).min().orElseThrow());
                var max=BlockVector3.at(selected.stream().mapToInt(BlockVector3::getBlockX).max().orElseThrow(),selected.stream().mapToInt(BlockVector3::getBlockY).max().orElseThrow(),selected.stream().mapToInt(BlockVector3::getBlockZ).max().orElseThrow());
                if(!clipboard.getDimensions().equals(max.subtract(min).add(1,1,1)))throw new IOException("Schematic selection dimensions differ");
                Map<BlockVector3,BaseBlock> originals=new LinkedHashMap<>();
                for(var pos:selected)originals.put(pos,clipboard.getFullBlock(pos.subtract(min).add(base)));
                return new Snapshot(id,worldId,originals);
            } finally {closeClipboard(clipboard);}
        } catch(RuntimeException e){throw new IOException("Invalid schematic",e);}
    }
    /** True also for incomplete/corrupt originals: callers must load, not recapture, these IDs. */
    public boolean exists(UUID id) {
        Objects.requireNonNull(id);
        return Files.exists(directory.resolve(id+".schem"))||Files.exists(directory.resolve(id+".snapshot"));
    }
    public Snapshot load(UUID id,World world)throws IOException {var snapshot=load(id);validateWorld(snapshot,world);return snapshot;}
    /** Lists committed object IDs without needing marker/anchor chunks. Load each to validate before recovery. */
    public Set<UUID> objectIds()throws IOException {
        try(var paths=Files.list(directory)) {
            Set<UUID> ids=new LinkedHashSet<>();
            for(var path:paths.filter(p->p.getFileName().toString().endsWith(".snapshot")).toList()) {
                String name=path.getFileName().toString();
                try{ids.add(UUID.fromString(name.substring(0,name.length()-9)));}catch(IllegalArgumentException e){throw new IOException("Invalid snapshot filename",e);}
            }
            return Set.copyOf(ids);
        }
    }
    /** All-or-nothing loaded-chunk preflight; never force-loads a chunk. */
    public boolean restoreLoaded(Snapshot snapshot,World world)throws IOException {
        requireMain();validateWorld(snapshot,world);
        for(var p:snapshot.originals.keySet())if(!world.isChunkLoaded(p.getBlockX()>>4,p.getBlockZ()>>4))return false;
        snapshot.restore(world,p->true);return true;
    }
    private static byte[] metadata(Snapshot snapshot)throws IOException {
        var bytes=new ByteArrayOutputStream();try(var out=new DataOutputStream(bytes)) {
            out.writeInt(1);out.writeLong(snapshot.objectId.getMostSignificantBits());out.writeLong(snapshot.objectId.getLeastSignificantBits());
            out.writeLong(snapshot.worldId.getMostSignificantBits());out.writeLong(snapshot.worldId.getLeastSignificantBits());out.writeInt(snapshot.originals.size());
            for(var p:snapshot.originals.keySet()){out.writeInt(p.getBlockX());out.writeInt(p.getBlockY());out.writeInt(p.getBlockZ());}
        }return bytes.toByteArray();
    }
    private static void validateWorld(Snapshot snapshot,World world) {
        Objects.requireNonNull(world);if(!world.getUID().equals(snapshot.worldId))throw new IllegalArgumentException("Snapshot belongs to another world");
        validateHeight(world,snapshot.originals.keySet());
    }
    private static void validateHeight(World world,Collection<BlockVector3> positions) {
        for(var p:positions)if(p.getBlockY()<world.getMinHeight()||p.getBlockY()>=world.getMaxHeight())throw new IllegalArgumentException("Selection outside world height");
    }
    private static void requireLoaded(World world,Collection<BlockVector3> positions) {
        for(var p:positions)if(!world.isChunkLoaded(p.getBlockX()>>4,p.getBlockZ()>>4))throw new IllegalStateException("Selected chunk not loaded");
    }
    private static void checkVolume(BlockVector3 min,BlockVector3 max) {
        long x=(long)max.getBlockX()-min.getBlockX()+1,y=(long)max.getBlockY()-min.getBlockY()+1,z=(long)max.getBlockZ()-min.getBlockZ()+1;
        if(x<=0||y<=0||z<=0||x>LIMIT||y>LIMIT||z>LIMIT||x*y>LIMIT||x*y*z>LIMIT)throw new IllegalArgumentException("Bounding box too large");
    }
    private static void requireMain(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Main thread required");}
    private static void closeClipboard(Clipboard clipboard)throws IOException {
        if(clipboard instanceof AutoCloseable closeable)try{closeable.close();}catch(Exception e){throw new IOException("Clipboard close failed",e);}
    }
    static Set<BlockVector3> select(Collection<OpenableRegion> regions) {
        if(regions==null||regions.isEmpty())throw new IllegalArgumentException("Empty selection");
        Set<BlockVector3> cells=new LinkedHashSet<>();
        for(var region:regions) {
            Objects.requireNonNull(region,"region");var min=region.getMin();var max=region.getMax();
            for(double v:new double[]{min.getX(),min.getY(),min.getZ(),max.getX(),max.getY(),max.getZ()})
                if(!Double.isFinite(v)||v<Integer.MIN_VALUE||v>=Integer.MAX_VALUE)throw new IllegalArgumentException("Unsupported coordinates");
            long sx=(long)max.getBlockX()-min.getBlockX()+1, sy=(long)max.getBlockY()-min.getBlockY()+1, sz=(long)max.getBlockZ()-min.getBlockZ()+1;
            if(sx>LIMIT||sy>LIMIT||sz>LIMIT||sx*sy>LIMIT||sx*sy*sz>LIMIT)throw new IllegalArgumentException("Selection too large");
            region.doForEveryBlock(v->{cells.add(BlockVector3.at(v.getBlockX(),v.getBlockY(),v.getBlockZ()));if(cells.size()>LIMIT)throw new IllegalArgumentException("Selection too large");});
        }
        if(cells.isEmpty())throw new IllegalArgumentException("Empty selection");
        return Collections.unmodifiableSet(cells);
    }
}
