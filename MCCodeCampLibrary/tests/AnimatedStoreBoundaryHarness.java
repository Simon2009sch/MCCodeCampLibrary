package me.simoncrafter.mCCodeCampLibrary.animation.schematic;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import org.bukkit.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import me.simoncrafter.mCCodeCampLibrary.obstical.AOpenableObject.OpenableRegion;
public class AnimatedStoreBoundaryHarness {
 static boolean main=true;
 @SuppressWarnings("unchecked") static <T>T proxy(Class<T> t,InvocationHandler h){return (T)Proxy.newProxyInstance(t.getClassLoader(),new Class[]{t},h);}
 public static void main(String[] args)throws Exception {
  var server=proxy(Server.class,(p,m,a)->switch(m.getName()) {
   case "isPrimaryThread" -> main;
   case "getName","getVersion","getBukkitVersion" -> "test";
   case "getLogger" -> java.util.logging.Logger.getLogger("animated-test");
   default -> null;
  });
  try{Bukkit.setServer(server);}catch(NoSuchElementException e){assert Bukkit.getServer()==server;}
  var dir=Files.createTempDirectory(Path.of(System.getenv("TMPDIR")),"animated-boundary-");
  var plugin=proxy(Plugin.class,(p,m,a)->m.getName().equals("getDataFolder")?dir.toFile():null);
  UUID worldId=UUID.randomUUID();
  var world=proxy(World.class,(p,m,a)->switch(m.getName()){
   case "getUID" -> worldId;
   case "getMinHeight" -> -64;
   case "getMaxHeight" -> 320;
   case "isChunkLoaded" -> false;
   default -> throw new AssertionError("Unexpected world access: "+m.getName());
  });
  try {
   var store=new AnimatedSchematicStore(plugin);
   assert !store.exists(UUID.randomUUID());
   UUID broken=UUID.randomUUID(); Files.write(dir.resolve("animated").resolve(broken+".schem"),new byte[]{1});
   assert store.exists(broken):"orphan must fail closed rather than recapture";
   var ctor=AnimatedSchematicStore.Snapshot.class.getDeclaredConstructor(UUID.class,UUID.class,Map.class);ctor.setAccessible(true);
   var empty=ctor.newInstance(UUID.randomUUID(),worldId,Map.of());
   try{empty.restoreBlock(world,new AnimatedSchematicStore.BlockPos(0,0,0));throw new AssertionError("unselected cell accepted");}catch(IllegalArgumentException expected){}
   try{store.capture(UUID.randomUUID(),world,List.of(new OpenableRegion(new Vector(),new Vector())));throw new AssertionError("unloaded accepted");}catch(IllegalStateException expected){}
   try{store.load(UUID.randomUUID());throw new AssertionError("missing accepted");}catch(java.io.IOException expected){}
   main=false;
   try{store.capture(UUID.randomUUID(),world,List.of(new OpenableRegion(new Vector(),new Vector())));throw new AssertionError("async accepted");}catch(IllegalStateException expected){}
   main=true;
   int[] updates={0},packets={0};
   var data=proxy(org.bukkit.block.data.BlockData.class,(p,m,a)->null);
   var state=proxy(org.bukkit.block.BlockState.class,(p,m,a)->{
    if(m.getName().equals("update")){assert Boolean.TRUE.equals(a[0]) && Boolean.FALSE.equals(a[1]);updates[0]++;return true;}
    return null;
   });
   var block=proxy(org.bukkit.block.Block.class,(p,m,a)->m.getName().equals("getState")?state:null);
   var viewer=proxy(org.bukkit.entity.Player.class,(p,m,a)->{
    if(m.getName().equals("sendMultiBlockChange")){assert ((Map<?,?>)a[0]).size()==1;packets[0]++;}return null;
   });
   var visibleWorld=proxy(World.class,(p,m,a)->switch(m.getName()){
    case "getBlockAt"->block;case "getPlayers"->List.of(viewer);case "hashCode"->1;case "equals"->p==a[0];default->null;
   });
   BlockRestoreUpdates.publish(visibleWorld,Map.of(new AnimatedSchematicStore.BlockPos(1,2,3),data));
   assert updates[0]==1 && packets[0]==1:"server restore must publish client block changes";
   System.out.println("PASS main-thread guards, unloaded capture refusal, missing load, client block updates");
  } finally {try(var paths=Files.walk(dir)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
 }
}
