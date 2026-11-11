package me.simoncrafter.mCCodeCampLibrary.animation;

import me.simoncrafter.mCCodeCampLibrary.animation.schematic.AnimatedSchematicStore.BlockSample;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/** Main-thread Paper adapter. Displays are ephemeral; schematics remain the sole source of originals. */
public final class SchematicScene implements AnimatedBlockRuntime.Scene {
    public static final String ENTITY_TAG = "mccodecamp_animated";
    static final String SESSION_TAG = "mccanim_" + java.util.UUID.randomUUID().toString().replace("-", "");
    private final World world;
    private final List<BlockSample> samples;
    private final BlockData replacement;
    private final Consumer<BlockSample> restore;
    private final Function<BlockSample,BlockData> data;
    private final NamespacedKey key;
    private final Map<Integer,Vector3f> centers=new HashMap<>();
    private final Map<Integer,BlockDisplay> displays = new HashMap<>();
    private final Map<Integer,Transformation> transforms=new HashMap<>();
    private long updateCount;

    public SchematicScene(Plugin plugin, World world, List<BlockSample> samples, BlockData replacement,
                          Consumer<BlockSample> restore) {
        this(plugin,world,samples,replacement,restore,BlockSample::blockData);
    }

    // Package boundary allows testing without a running Bukkit block-data parser.
    SchematicScene(Plugin plugin, World world, List<BlockSample> samples, BlockData replacement,
                   Consumer<BlockSample> restore, Function<BlockSample,BlockData> data) {
        this.world=world; this.samples=List.copyOf(samples); this.replacement=replacement.clone();
        this.restore=restore; this.data=data; key=new NamespacedKey(plugin,"animated_display");
    }

    public int size() { return samples.size(); }
    public Vector3f originalCenter(int i) { var b=samples.get(i); return new Vector3f(b.x()+.5f,b.y()+.5f,b.z()+.5f); }

    public boolean loaded(int i,Vector3f center) {
        var b=samples.get(i);
        return world.isChunkLoaded(b.x()>>4,b.z()>>4)
                && world.isChunkLoaded(((int)Math.floor(center.x))>>4,((int)Math.floor(center.z))>>4);
    }

    public void original(int i) {
        var b=samples.get(i);
        if (world.isChunkLoaded(b.x()>>4,b.z()>>4)) restore.accept(b);
    }

    public void replacement(int i) {
        var b=samples.get(i);
        if (!world.isChunkLoaded(b.x()>>4,b.z()>>4)) throw new IllegalStateException("Source chunk unloaded");
        world.getBlockAt(b.x(),b.y(),b.z()).setBlockData(replacement,false);
    }

    public void display(int i,Vector3f center,Quaternionf left,Vector3f scale,Quaternionf right) {
        // Keep lighting/chunk ownership at this block's original integer position, never the model pivot.
        var sample=samples.get(i);
        Vector3f origin=new Vector3f(sample.x(),sample.y(),sample.z());
        Location loc=new Location(world,origin.x,origin.y,origin.z);
        centers.put(i,new Vector3f(center));
        BlockDisplay display=displays.get(i);
        if (display==null || !display.isValid()) {
            BlockData block=data.apply(samples.get(i));
            display=world.spawn(loc,BlockDisplay.class,e->{
                e.setPersistent(false);
                e.addScoreboardTag(ENTITY_TAG);
                e.addScoreboardTag(SESSION_TAG);
                e.getPersistentDataContainer().set(key,PersistentDataType.BYTE,(byte)1);
                e.setBlock(block);
                e.setViewRange(4);

            });
            displays.put(i,display);
            transforms.remove(i);
        }
        // Native blocks start at their lower corner; transform that corner around the block center.
        Vector3f corner=new Vector3f(-.5f);
        right.transform(corner); corner.mul(scale); left.transform(corner);
        corner.add(center).sub(origin);
        Transformation transform=new Transformation(corner,new Quaternionf(left),new Vector3f(scale),new Quaternionf(right));
        if(transform.equals(transforms.get(i)))return;
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(1);
        display.setTransformation(transform);
        updateCount++;
        transforms.put(i,transform);
    }

    public int displayCount() { return displays.size(); }
    public long updateCount() { return updateCount; }

    public boolean touchesChunk(int x,int z) {
        for (var b:samples) if ((b.x()>>4)==x && (b.z()>>4)==z) return true;
        for (var p:centers.values())
            if (((int)Math.floor(p.x)>>4)==x && ((int)Math.floor(p.z)>>4)==z)return true;
        return false;
    }

    public void removeDisplay(int i) {
        centers.remove(i);transforms.remove(i);
        BlockDisplay display=displays.remove(i);
        if (display!=null) display.remove();
    }
}
