package me.simoncrafter.mCCodeCampLibrary.animation;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.*;
import me.simoncrafter.mCCodeCampLibrary.animation.schematic.AnimatedSchematicStore.BlockSample;

/** Paper boundary doubles execute the production scene adapter; no live rendering claim. */
public class SchematicSceneHarness {
    @SuppressWarnings("unchecked") static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},handler);
    }
    static Object fallback(Class<?> t) { if(!t.isPrimitive()||t==void.class)return null; if(t==boolean.class)return false; if(t==float.class)return 0f; if(t==double.class)return 0d; if(t==long.class)return 0L; return 0; }
    public static void main(String[] args) {
        AtomicInteger restores=new AtomicInteger(), clears=new AtomicInteger(), removals=new AtomicInteger(), teleports=new AtomicInteger(), transformPackets=new AtomicInteger();
        Transformation[] transform={null}; boolean[] persistent={true}; org.bukkit.Location[] anchor={null};
        PersistentDataContainer pdc=proxy(PersistentDataContainer.class,(p,m,a)->fallback(m.getReturnType()));
        BlockData data=proxy(BlockData.class,(p,m,a)->m.getName().equals("clone")?p:fallback(m.getReturnType()));
        BlockDisplay display=proxy(BlockDisplay.class,(p,m,a)->{switch(m.getName()){
            case "getPersistentDataContainer":return pdc;
            case "isValid":return true;
            case "setPersistent":persistent[0]=(boolean)a[0];break;
            case "setTransformation":transform[0]=(Transformation)a[0];transformPackets.incrementAndGet();break;
            case "remove":removals.incrementAndGet();break;
            case "teleport":teleports.incrementAndGet();return true;
        }return fallback(m.getReturnType());});
        Block block=proxy(Block.class,(p,m,a)->{if(m.getName().equals("setBlockData"))clears.incrementAndGet();return fallback(m.getReturnType());});
        World world=proxy(World.class,(p,m,a)->{switch(m.getName()){
            case "isChunkLoaded":return true;
            case "getBlockAt":return block;
            case "spawn":anchor[0]=(org.bukkit.Location)a[0];((java.util.function.Consumer<BlockDisplay>)a[2]).accept(display);return display;
        }return fallback(m.getReturnType());});
        Plugin plugin=proxy(Plugin.class,(p,m,a)->m.getReturnType()==String.class?"animationtests":fallback(m.getReturnType()));
        SchematicScene scene=new SchematicScene(plugin,world,List.of(new BlockSample(0,0,0,"minecraft:stone")),data,
                sample->restores.incrementAndGet(),sample->data);
        scene.replacement(0);
        scene.display(0,new Vector3f(.5f),new Quaternionf(),new Vector3f(1),new Quaternionf());
        if(persistent[0])throw new AssertionError("Display persists independently of schematic");
        if(transform[0]==null||transform[0].getTranslation().distance(new Vector3f(0))>.001f)throw new AssertionError("Display translation is not identity at its original block");
        if(anchor[0].getX()!=0||anchor[0].getY()!=0||anchor[0].getZ()!=0)throw new AssertionError("Entity is not anchored at its original block corner");
        scene.original(0);
        scene.display(0,new Vector3f(5.5f),new Quaternionf(),new Vector3f(1),new Quaternionf());
        if(teleports.get()!=0)throw new AssertionError("Animation teleports the display anchor instead of translating it");
        if(transform[0].getTranslation().distance(new Vector3f(5f))>.001f)throw new AssertionError("World movement is missing from translation");
        scene.display(0,new Vector3f(5.5f),new Quaternionf(),new Vector3f(1),new Quaternionf());
        if(transformPackets.get()!=2)throw new AssertionError("Identical transform resent to client");
        scene.removeDisplay(0);scene.removeDisplay(0);
        if(restores.get()!=1||clears.get()!=1||removals.get()!=1)throw new AssertionError("Scene lifecycle incorrect");
        System.out.println("SchematicSceneHarness passed");
    }
}
