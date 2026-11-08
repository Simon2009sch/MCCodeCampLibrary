package me.simoncrafter.mCCodeCampLibrary.projectile;
import me.simoncrafter.mCCodeCampLibrary.projectile.effect.MeteorVisuals;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.world.*;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.*;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.bukkit.util.RayTraceResult;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.joml.Vector3f;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;
/** Actual production meteor/CDL execution at Paper boundaries, not client rendering. */
public class MeteorVisualHarness {
 static int assertions;
 static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
 static void near(double a,double b,String message){check(Math.abs(a-b)<.0002,message+": "+a+" != "+b);}
 @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,InvocationHandler handler){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->{
  if(m.getName().equals("equals"))return p==a[0];if(m.getName().equals("hashCode"))return System.identityHashCode(p);return handler.invoke(p,m,a);
 });}
 static Plugin plugin=proxy(Plugin.class,(p,m,a)->switch(m.getName()){case "getName","namespace"->"test";case "getLogger"->Logger.getLogger("meteor");case "isEnabled"->true;default->null;});
 static class Visual {
  Location anchor;Transformation transform;Material material;int removes,duration=-1,delay=-1;boolean persistent=true;
  Location actual(){Vector3f c=transform.getLeftRotation().transform(transform.getRightRotation().transform(new Vector3f(.5f)).mul(transform.getScale())).add(transform.getTranslation());return anchor.clone().add(c.x,c.y,c.z);}
 }
 static class Fixture {
  List<Visual> visuals=new ArrayList<>();List<Player> players=new ArrayList<>();World world;boolean loaded=true,hit;Map<Integer,Material> column=new HashMap<>();int highest=70;
  Fixture(){
   PersistentDataContainer data=proxy(PersistentDataContainer.class,(p,m,a)->null);
   Block block=proxy(Block.class,(p,m,a)->null);
   world=proxy(World.class,(p,m,a)->switch(m.getName()){
    case "getPlayers"->players;
    case "createExplosion","setBlockData"->throw new AssertionError("visuals must not mutate terrain or explode");
    case "getBlockAt"->{check(loaded,"no terrain reads in unloaded chunk");int y=(int)a[1];Material material=column.getOrDefault(y,y<=70?Material.STONE:Material.AIR);yield proxy(Block.class,(q,n,b)->switch(n.getName()){case "getType"->material;case "isEmpty"->material==Material.AIR;case "isPassable"->switch(material){case STONE,BAMBOO,AZALEA,FLOWERING_AZALEA,RED_MUSHROOM_BLOCK,BROWN_MUSHROOM_BLOCK->false;default->true;};case "isLiquid"->material==Material.WATER;default->throw new AssertionError("unexpected block operation "+n.getName());});}
    case "isChunkLoaded"->loaded;case "getHighestBlockYAt"->{check(loaded,"loaded highest lookup");yield highest;}case "getMinHeight"->-64;case "getMaxHeight"->320;
    case "rayTraceBlocks"->hit?new RayTraceResult(((Location)a[0]).toVector().add(((Vector)a[1]).clone().multiply((double)a[2]/2)),block,BlockFace.UP):null;
    case "spawn"->{
     check(a[1]==BlockDisplay.class,"native block display");Visual v=new Visual();v.anchor=((Location)a[0]).clone();
     BlockDisplay entity=proxy(BlockDisplay.class,(q,n,b)->switch(n.getName()){
      case "isValid"->v.removes==0;case "getPersistentDataContainer"->data;
      case "getLocation"->v.anchor.clone();case "getWorld"->world;
      case "setPersistent"->{v.persistent=(boolean)b[0];yield null;}
      case "setBlock"->{v.material=((BlockData)b[0]).getMaterial();yield null;}
      case "setTransformation"->{v.transform=(Transformation)b[0];yield null;}
      case "setInterpolationDuration"->{v.duration=(int)b[0];yield null;}
      case "setInterpolationDelay"->{v.delay=(int)b[0];yield null;}
      case "teleport","teleportAsync"->throw new AssertionError("raw anchor must not teleport");
      case "remove"->{v.removes++;yield null;}default->null;
     });
     if(a.length>2 && a[2] instanceof java.util.function.Consumer<?> c)((java.util.function.Consumer<BlockDisplay>)c).accept(entity);
     visuals.add(v);yield entity;
    }default->null;
   });
  }
  Location center(){return new Location(world,100,80,200,75,30);}
  void player(double x,double y,double z){Location l=new Location(world,x,y,z,123,42);players.add(proxy(Player.class,(p,m,a)->m.getName().equals("getLocation")?l.clone():null));}
  void clean(){for(Visual v:visuals){check(v.removes==1,"exactly-once native remove");check(!v.persistent,"native persistence false");}}
 }
 static void setup(){
  PluginManager pm=proxy(PluginManager.class,(p,m,a)->{
   if(m.getName().equals("registerEvents"))for(Method method:a[0].getClass().getMethods())if(method.isAnnotationPresent(EventHandler.class)){
    EventHandler h=method.getAnnotation(EventHandler.class);HandlerList list=(HandlerList)method.getParameterTypes()[0].getMethod("getHandlerList").invoke(null);
    list.register(new RegisteredListener((Listener)a[0],(l,e)->{try{method.invoke(l,e);}catch(Exception ex){throw new EventException(ex);}},h.priority(),plugin,h.ignoreCancelled()));
   }return null;
  });
  Server server=proxy(Server.class,(p,m,a)->switch(m.getName()){
   case "getName","getVersion","getBukkitVersion"->"test";case "getLogger"->Logger.getLogger("meteor");case "isPrimaryThread"->true;case "getPluginManager"->pm;
   case "getScheduler"->throw new AssertionError("must not schedule tasks");
   case "createBlockData"->{Material material=(Material)a[0];yield proxy(BlockData.class,(q,n,b)->n.getName().equals("getMaterial")?material:null);}default->null;
  });
  try{Bukkit.setServer(server);}catch(NoSuchElementException ex){check(Bukkit.getServer()==server,"API-only server setup");}
 }
 static void dispatch(Event e)throws Exception{for(RegisteredListener l:e.getHandlers().getRegisteredListeners())l.callEvent(e);}
 static Chunk chunk(World w,int x,int z){return proxy(Chunk.class,(p,m,a)->switch(m.getName()){case "getWorld"->w;case "getX"->x;case "getZ"->z;default->null;});}
 static void timeline(){
  Fixture f=new Fixture();f.player(400,80,400);f.player(96,65,192);Location center=f.center();MeteorVisuals effects=new MeteorVisuals(plugin);int[] burns={0};
  effects.startImpact(center,true,(l,r)->{
   burns[0]++;check(l.getWorld()==f.world,"burn world");near(l.getY(),71,"burn surface height");double distance=Math.hypot(l.getX()-100,l.getZ()-200);
   check(distance<=50.0002,"burn first fifty only");near(r,1+4*distance/50,"burn radius tracks scale");
   check(f.visuals.stream().anyMatch(v->v.removes==0 && v.actual().distanceSquared(l)<1e-7),"callback equals actual Paper transform center");
  });
  double[] previousDistance=new double[96],previousSpeed=new double[96],previousSpin=new double[96],previousAngularSpeed=new double[96];
  Arrays.fill(previousSpeed,Double.POSITIVE_INFINITY);Arrays.fill(previousAngularSpeed,Double.POSITIVE_INFINITY);
  Set<Material> seen=EnumSet.noneOf(Material.class);
  for(int tick=0;tick<165;tick++){
   effects.tick();check(f.visuals.size()==(tick<6?32:tick<12?64:96),"32 rays x 3 rings stagger 6");
   for(int i=0;i<f.visuals.size();i++){
    Visual v=f.visuals.get(i);int elapsed=tick-(i/32)*6;near(v.anchor.getX(),96,"nearest fixed anchor X");near(v.anchor.getY(),65,"nearest fixed anchor Y");near(v.anchor.getYaw(),0,"neutral yaw");near(v.anchor.getPitch(),0,"neutral pitch");
    if(elapsed>150){check(v.removes==1,"remove after finite 150-tick travel");continue;}
    double angle=2*Math.PI*((i%32)+(i/32)/3.0)/32;
    double distance=150*(2*(elapsed/150.0)-Math.pow(elapsed/150.0,2));
    near(v.actual().getX(),100+Math.cos(angle)*distance,"straight radial X without orbit");near(v.actual().getZ(),200+Math.sin(angle)*distance,"straight radial Z without orbit");
    double actualDistance=Math.hypot(v.actual().getX()-100,v.actual().getZ()-200);
    near(actualDistance,distance,"quadratic ease-out reaches 150");near(v.actual().getY(),71,"native surface center");near(v.transform.getScale().x,Math.min(10,2+8*distance/50),"scale 2 to 10");
    double spin=.04*(elapsed-elapsed*elapsed/300.0);
    Vector3f facing=v.transform.getLeftRotation().transform(new Vector3f(1,0,0));
    check(Math.abs(facing.x-Math.cos(angle+spin))<.001,"own-center rotating facing X");check(Math.abs(facing.z+Math.sin(angle+spin))<.001,"own-center rotating facing Z (CDL matrix decomposition tolerance)");
    if(elapsed>0){double speed=actualDistance-previousDistance[i],angularSpeed=spin-previousSpin[i];check(speed>0 && speed<previousSpeed[i]+.0002,"outward speed decreases monotonically");check(angularSpeed>0 && angularSpeed<previousAngularSpeed[i],"angular speed declines but rotation is nonstatic");previousSpeed[i]=speed;previousAngularSpeed[i]=angularSpeed;}
    previousDistance[i]=actualDistance;previousSpin[i]=spin;
    check(v.duration==1 && v.delay==0,"one-tick animation");Material expected=distance<15?Material.MAGMA_BLOCK:distance<35?Material.YELLOW_STAINED_GLASS:distance<65?Material.ORANGE_STAINED_GLASS:distance<95?Material.WHITE_STAINED_GLASS:distance<125?Material.GRAY_STAINED_GLASS:Material.BLACK_STAINED_GLASS;check(v.material==expected,"six-stage material progression");seen.add(v.material);
   }
  }
  check(seen.size()==6,"all six stages observed before removal");check(burns[0]==32*3*28,"all burn callbacks within first fifty eased blocks");check(!effects.isActive(),"natural expiry");near(center.getY(),80,"caller location unchanged");effects.close();effects.close();f.clean();
 }
 static void terrain(){
  for(Material ignored:new Material[]{Material.OAK_LEAVES,Material.OAK_LOG,Material.OAK_WOOD,Material.STRIPPED_OAK_LOG,Material.CRIMSON_STEM,Material.WARPED_HYPHAE,Material.SHORT_GRASS,Material.FERN,Material.VINE,Material.DANDELION,Material.MANGROVE_ROOTS,Material.BAMBOO,Material.AZALEA,Material.FLOWERING_AZALEA,Material.RED_MUSHROOM_BLOCK,Material.BROWN_MUSHROOM_BLOCK}){
   Fixture f=new Fixture();f.highest=76;f.column.put(76,ignored);f.column.put(75,Material.OAK_LEAVES);f.column.put(74,Material.OAK_LOG);
   MeteorVisuals effects=new MeteorVisuals(plugin);effects.startImpact(f.center(),false,(l,r)->{});effects.tick();for(Visual v:f.visuals)near(v.actual().getY(),71,"ignore tree/foliage "+ignored);effects.close();f.clean();
  }
  Fixture water=new Fixture();water.highest=80;for(int y=71;y<=80;y++)water.column.put(y,Material.WATER);
  MeteorVisuals effects=new MeteorVisuals(plugin);effects.startImpact(water.center(),true,(l,r)->near(l.getY(),81,"burn at water surface not seabed"));effects.tick();for(Visual v:water.visuals)near(v.actual().getY(),81,"cloud at water surface not seabed");effects.close();water.clean();
 }
 static void callbacks(){
  Fixture f=new Fixture();MeteorVisuals effects=new MeteorVisuals(plugin);int[] calls={0};
  effects.startImpact(f.center(),true,(l,r)->{calls[0]++;effects.tick();effects.close();});effects.startImpact(f.center(),true,(l,r)->{throw new AssertionError("closed sibling callback");});
  effects.tick();check(calls[0]==1 && !effects.isActive(),"callback close and nested tick safe");int count=f.visuals.size();effects.tick();check(f.visuals.size()==count,"no recreation after close");effects.close();f.clean();
  try{effects.startImpact(f.center(),false,(l,r)->{});throw new AssertionError("closed service accepted wave");}catch(IllegalStateException expected){}
  Fixture failing=new Fixture();MeteorVisuals failingEffects=new MeteorVisuals(plugin);RuntimeException boom=new IllegalArgumentException("burn failure");
  failingEffects.startImpact(failing.center(),true,(l,r)->{throw boom;});
  try{failingEffects.tick();throw new AssertionError("callback failure lost");}catch(RuntimeException actual){check(actual==boom,"original callback failure propagated");}
  check(!failingEffects.isActive(),"failed callback releases wave");failingEffects.close();failing.clean();
  Fixture unloading=new Fixture();MeteorVisuals unloadingEffects=new MeteorVisuals(plugin);int[] unloadCalls={0};
  unloadingEffects.startImpact(unloading.center(),true,(l,r)->{unloadCalls[0]++;unloadingEffects.onWorldUnload(new WorldUnloadEvent(unloading.world));});
  unloadingEffects.tick();check(unloadCalls[0]==1 && !unloadingEffects.isActive(),"callback-driven unload does not continue spawning");unloadingEffects.tick();unloadingEffects.close();unloading.clean();
  Fixture harmless=new Fixture();MeteorVisuals harmlessEffects=new MeteorVisuals(plugin);harmlessEffects.startImpact(harmless.center(),false,(l,r)->{throw new AssertionError("visual-only burn");});for(int i=0;i<90;i++)harmlessEffects.tick();harmlessEffects.close();harmless.clean();
 }
 static void projectile(){
  Fixture f=new Fixture();f.player(90,64,190);ProjectileManager manager=new ProjectileManager(plugin);List<Location> impacts=new ArrayList<>();
  MeteorProjectile meteor=new MeteorProjectile(impacts::add);meteor.shoot(manager,f.center(),new Vector(0,-2,0),null);Visual v=f.visuals.getLast();near(v.anchor.getX(),90,"falling nearest anchor");near(v.anchor.getYaw(),0,"falling neutral yaw");near(v.actual().getY(),80,"initial centered meteor");check(v.material==Material.MAGMA_BLOCK,"falling magma");manager.tick();near(v.actual().getY(),78,"rotating meteor endpoint center");check(v.duration==1,"falling one-tick move");meteor.remove();meteor.remove();check(impacts.isEmpty(),"remove no impact");
  meteor=new MeteorProjectile(impacts::add);meteor.setMaxTicks(1);meteor.shoot(manager,f.center(),new Vector(0,-2,0),null);manager.tick();check(!meteor.isActive() && impacts.isEmpty(),"expiry cleanup no impact");
  f.hit=true;meteor=new MeteorProjectile(impacts::add);meteor.shoot(manager,f.center(),new Vector(0,-2,0),null);manager.tick();check(impacts.size()==1 && !meteor.isActive(),"hit callback exactly once");near(impacts.getFirst().getY(),79,"exact hit location");manager.tick();check(impacts.size()==1,"no repeat hit");f.hit=false;
  meteor=new MeteorProjectile(impacts::add);meteor.shoot(manager,f.center(),new Vector(0,-2,0),null);manager.onWorldUnload(new WorldUnloadEvent(f.world));check(!meteor.isActive(),"projectile world unload cleanup");
  meteor=new MeteorProjectile(impacts::add);meteor.shoot(manager,f.center(),new Vector(0,-2,0),null);manager.onChunkUnload(new ChunkUnloadEvent(chunk(f.world,6,12)));check(!meteor.isActive(),"projectile flight chunk unload cleanup");
  meteor=new MeteorProjectile(impacts::add);meteor.shoot(manager,f.center(),new Vector(0,-2,0),null);manager.close();f.clean();
 }
 static void unloads()throws Exception{
  Fixture f=new Fixture();f.player(16,65,32);MeteorVisuals effects=new MeteorVisuals(plugin);effects.startImpact(f.center(),false,(l,r)->{});effects.tick();
  WorldUnloadEvent cancelled=new WorldUnloadEvent(f.world);cancelled.setCancelled(true);dispatch(cancelled);check(effects.isActive(),"cancelled world unload must retain visuals");
  dispatch(new ChunkUnloadEvent(chunk(f.world,6,12)));check(effects.isActive(),"impact chunk is not anchor chunk");dispatch(new ChunkUnloadEvent(chunk(f.world,1,2)));check(!effects.isActive(),"anchor chunk unload cleanup");effects.close();f.clean();
  Fixture other=new Fixture();effects=new MeteorVisuals(plugin);effects.startImpact(other.center(),false,(l,r)->{});effects.tick();dispatch(new WorldUnloadEvent(f.world));check(effects.isActive(),"unrelated world ignored");dispatch(new WorldUnloadEvent(other.world));check(!effects.isActive(),"world unload cleanup");effects.close();other.clean();
  Fixture missing=new Fixture();effects=new MeteorVisuals(plugin);effects.startImpact(missing.center(),false,(l,r)->{});effects.tick();missing.loaded=false;effects.tick();check(!effects.isActive(),"unloaded anchor cleanup");effects.close();missing.clean();
  Fixture disabled=new Fixture();effects=new MeteorVisuals(plugin);effects.startImpact(disabled.center(),false,(l,r)->{});effects.tick();dispatch(new PluginDisableEvent(plugin));check(!effects.isActive(),"plugin disable cleanup");disabled.clean();
 }
 public static void main(String[] args)throws Exception{setup();timeline();terrain();callbacks();projectile();unloads();check(HandlerList.getRegisteredListeners(plugin).isEmpty(),"no leaked listeners");System.out.println("Meteor production/CDL boundary checks passed: "+assertions+" assertions; timeline, transforms, callbacks, lifecycle; no scheduled tasks");}
}
