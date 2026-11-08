package me.simoncrafter.mCCodeCampLibrary.projectile;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.*;
import org.bukkit.util.*;
import org.bukkit.util.Vector;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

/** Actual snowball + CDL wrapper; Bukkit entity/world boundary doubles, not a rendering test. */
public class SnowballDisplayHarness {
    static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
    @SuppressWarnings("unchecked") static <T> T proxy(Class<T> type,InvocationHandler action) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->{
            if(m.getName().equals("hashCode")) return System.identityHashCode(p);
            if(m.getName().equals("equals")) return p==a[0];
            return action.invoke(p,m,a);
        });
    }
    static class Visual {
        Location origin;
        BlockData block;
        Transformation transform;
        int duration=-1, removes;
        boolean persistent=true;
        BlockDisplay entity;
    }
    static boolean near(float a,double b) {return Math.abs(a-b)<1e-5;}
    static org.joml.Vector3f center(Transformation transform) {
        return transform.getLeftRotation().transform(new org.joml.Vector3f(.5f).mul(transform.getScale())).add(transform.getTranslation());
    }
    static org.joml.Vector3f forward(Transformation transform) {
        return transform.getLeftRotation().transform(new org.joml.Vector3f(0,0,1));
    }
    public static void main(String[] args) {
        List<Visual> visuals=new ArrayList<>();
        boolean[] hit={false};
        List<Vector> burst=new ArrayList<>();
        List<Object> sources=new ArrayList<>();
        List<Location> impactParticles=new ArrayList<>();
        org.bukkit.entity.LivingEntity shooter=proxy(org.bukkit.entity.LivingEntity.class,(p,m,a)->null);
        BlockData snow=proxy(BlockData.class,(p,m,a)->m.getName().equals("getMaterial")?Material.SNOW_BLOCK:null);
        Block block=proxy(Block.class,(p,m,a)->null);
        PersistentDataContainer data=proxy(PersistentDataContainer.class,(p,m,a)->null);
        World world=proxy(World.class,(p,m,a)->switch(m.getName()) {
            case "getMinHeight"->-64; case "getMaxHeight"->320; case "isChunkLoaded"->true;
            case "spawnParticle"->{if(a[0]==Particle.ITEM_SNOWBALL) impactParticles.add(((Location)a[1]).clone());yield null;}
            case "spawn"->{
                if(a[1]==org.bukkit.entity.Snowball.class) {
                    check(((Location)a[0]).toVector().equals(new Vector(10.5,64,20)),"burst at precise impact");
                    boolean[] tagged={false}, temporary={false};
                    PersistentDataContainer marker=proxy(PersistentDataContainer.class,(q,n,b)->{
                        if(n.getName().equals("set")) {check(b[0].equals(new NamespacedKey("mccodecamp","snowball_burst")) && b[1]==org.bukkit.persistence.PersistentDataType.BOOLEAN && b[2].equals(true),"burst marker");tagged[0]=true;}return null;
                    });
                    org.bukkit.entity.Snowball ball=proxy(org.bukkit.entity.Snowball.class,(q,n,b)->switch(n.getName()) {
                        case "setVelocity"->{burst.add(((Vector)b[0]).clone());yield null;}
                        case "setShooter"->{sources.add(b[0]);yield null;}
                        case "setPersistent"->{temporary[0]=!(boolean)b[0];yield null;}
                        case "getPersistentDataContainer"->marker;
                        default->null;
                    });
                    if(a.length>2 && a[2] instanceof java.util.function.Consumer<?> consumer) ((java.util.function.Consumer<org.bukkit.entity.Snowball>)consumer).accept(ball);
                    check(tagged[0] && temporary[0],"native burst tagged and nonpersistent before insertion");
                    yield ball;
                }
                Visual visual=new Visual(); visual.origin=((Location)a[0]).clone();
                check(a[1]==BlockDisplay.class,"a block display is spawned");
                visual.entity=proxy(BlockDisplay.class,(entity,method,args2)->switch(method.getName()) {
                    case "isValid"->visual.removes==0;
                    case "setBlock"->{visual.block=(BlockData)args2[0];yield null;}
                    case "setTransformation"->{visual.transform=(Transformation)args2[0];yield null;}
                    case "setInterpolationDuration"->{visual.duration=(int)args2[0];yield null;}
                    case "setPersistent"->{visual.persistent=(boolean)args2[0];yield null;}
                    case "getPersistentDataContainer"->data;
                    case "remove"->{visual.removes++;yield null;}
                    default->null;
                });
                visuals.add(visual);yield visual.entity;
            }
            case "rayTraceBlocks"->hit[0]?new RayTraceResult(((Location)a[0]).toVector().add(((Vector)a[1]).clone().multiply((double)a[2]/2)),block,BlockFace.WEST):null;
            default->null;
        });
        boolean[] cancelHit={false};
        PluginManager pm=proxy(PluginManager.class,(p,m,a)->{
            if(m.getName().equals("callEvent") && cancelHit[0] && a[0] instanceof me.simoncrafter.mCCodeCampLibrary.projectile.event.ProjectileHitEvent event) event.setCancelled(true);
            return null;
        });
        Logger logger=Logger.getLogger("snowball-display-test");
        Server server=proxy(Server.class,(p,m,a)->switch(m.getName()) {
            case "getName","getVersion","getBukkitVersion"->"test"; case "getLogger"->logger;
            case "isPrimaryThread"->true; case "getPluginManager"->pm; case "createBlockData"->snow; default->null;
        });
        try {Bukkit.setServer(server);} catch(NoSuchElementException expected) {check(Bukkit.getServer()==server,"API setup");}
        Plugin plugin=proxy(Plugin.class,(p,m,a)->switch(m.getName()) {case "getName","namespace"->"test";case "getLogger"->logger;default->null;});
        ProjectileManager manager=new ProjectileManager(plugin);
        Location start=new Location(world,10,64,20,90,30);
        SnowballProjectile projectile=new SnowballProjectile(); projectile.shoot(manager,start,new Vector(1,0,0),null);
        check(visuals.size()==1,"snowball spawns exactly one snow block display on shoot");
        Visual first=visuals.getFirst();
        check(first.block==snow && first.block.getMaterial()==Material.SNOW_BLOCK,"snow block material");
        check(first.origin.toVector().equals(start.toVector()) && first.origin.getYaw()==0 && first.origin.getPitch()==0 && center(first.transform).length()<1e-5,"snow block centered at projectile spawn without inheriting shooter rotation");
        check(near(forward(first.transform).x,1) && near(forward(first.transform).y,0),"snow block faces initial travel direction");
        check(!first.persistent,"temporary snow visual is not saved into the world");
        manager.tick();
        check(first.duration==1 && near(center(first.transform).x,1),"animated move to first tick position");
        manager.tick();
        check(near(center(first.transform).x,1.99) && near(center(first.transform).y,-.03),"display follows snowball gravity relative to spawn anchor");
        org.joml.Vector3f expectedDirection=new org.joml.Vector3f(.99f,-.03f,0).normalize();
        check(forward(first.transform).distance(expectedDirection)<1e-5,"snow block pitches with its actual travelled segment");
        check(start.getX()==10 && start.getY()==64,"caller spawn location is unchanged");
        projectile.remove(); projectile.remove(); check(first.removes==1,"manual removal releases visual exactly once");
        projectile=new SnowballProjectile(); projectile.setMaxTicks(1); projectile.shoot(manager,start,new Vector(1,0,0),null); manager.tick();
        check(visuals.getLast().removes==1,"expiry releases visual");
        hit[0]=true; projectile=new SnowballProjectile(); projectile.shoot(manager,start,new Vector(1,0,0),null); manager.tick();
        check(visuals.getLast().removes==1 && !projectile.isActive(),"impact releases visual");
        check(burst.size()==100,"accepted impact spawns exactly 100 native snowballs");
        check(impactParticles.size()==1 && impactParticles.getFirst().toVector().equals(new Vector(10.5,64,20)),"impact particles at collision point");
        Vector sum=new Vector(); boolean[] octants=new boolean[8];
        for(Vector v:burst) {check(Math.abs(v.length()-1)<1e-9,"uniform speed");sum.add(v);octants[(v.getX()>0?1:0)|(v.getY()>0?2:0)|(v.getZ()>0?4:0)]=true;}
        for(boolean covered:octants) check(covered,"all sphere octants covered");
        check(sum.length()<1,"balanced sphere directions");
        check(sources.isEmpty(),"null shooter not replaced by a fabricated source");
        manager.tick();check(burst.size()==100,"no duplicate emission after removal");
        projectile=new SnowballProjectile();projectile.shoot(manager,start,new Vector(1,0,0),shooter);manager.tick();
        check(burst.size()==200 && sources.size()==100 && sources.stream().allMatch(source->source==shooter),"original living shooter preserved");
        cancelHit[0]=true;
        projectile=new SnowballProjectile();projectile.shoot(manager,start,new Vector(1,0,0),null);manager.tick();projectile.remove();
        check(burst.size()==200 && impactParticles.size()==2,"cancelled impacts never emit burst or impact particles");
        check(visuals.getLast().removes==1,"cancelled impact visual still has normal cleanup");
        cancelHit[0]=false;
        hit[0]=false;
        for (Vector direction : List.of(new Vector(0,1,0),new Vector(0,-1,0),new Vector(0,0,-1),new Vector())) {
            projectile=new SnowballProjectile(); projectile.shoot(manager,start,direction,null);
            var rotation=visuals.getLast().transform.getLeftRotation();
            check(Float.isFinite(rotation.x) && Float.isFinite(rotation.y) && Float.isFinite(rotation.z) && Float.isFinite(rotation.w),"vertical/backwards/zero direction stays finite");
            if (direction.lengthSquared()>0) check(forward(visuals.getLast().transform).distance(direction.toVector3f().normalize())<1e-5,"vertical and backwards facing");
            projectile.remove();
        }
        projectile=new SnowballProjectile();projectile.shoot(manager,start,new Vector(1,0,0),null);manager.close();
        check(visuals.getLast().removes==1,"manager shutdown releases visual");
        System.out.println("Snowball display boundary checks passed: spawn, centered snow, animated movement, gravity, cleanup");
    }
}
