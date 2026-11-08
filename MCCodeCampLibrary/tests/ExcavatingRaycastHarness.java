package me.simoncrafter.mCCodeCampLibrary.projectile;

import org.bukkit.*;
import org.bukkit.plugin.*;
import org.bukkit.util.Vector;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

/** Actual production movement/range/callback, mocked Bukkit boundary only. */
public class ExcavatingRaycastHarness {
    static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
    @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,InvocationHandler action) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->{
            if(m.getName().equals("hashCode"))return System.identityHashCode(p);
            if(m.getName().equals("equals"))return p==a[0];
            return action.invoke(p,m,a);
        });
    }
    public static void main(String[] args) throws Exception {
        Logger logger=Logger.getLogger("ray-boundary");
        Server server=proxy(Server.class,(p,m,a)->switch(m.getName()) {
            case "getName","getVersion","getBukkitVersion"->"test"; case "getLogger"->logger;
            case "isPrimaryThread"->true;default->null;
        });
        try {Bukkit.setServer(server);}catch(NoSuchElementException expected){check(Bukkit.getServer()==server,"API bootstrap");}
        Plugin plugin=proxy(Plugin.class,(p,m,a)->m.getName().equals("getLogger")?logger:null);
        int[] queries={0}; boolean[] clipped={false};
        World world=proxy(World.class,(p,m,a)->switch(m.getName()) {
            case "getMinHeight"->-64;case "getMaxHeight"->320;
            case "isChunkLoaded"->!clipped[0] || (int)a[0]<1;
            case "rayTraceBlocks","rayTraceEntities"->{queries[0]++;throw new AssertionError("excavating ray must ignore blocking collision");}
            default->null;
        });
        ProjectileManager manager=new ProjectileManager(plugin);
        List<Location[]> calls=new ArrayList<>();
        // Reflection lets this test compile before the production class exists (RED).
        Class<?> type=Class.forName("me.simoncrafter.mCCodeCampLibrary.projectile.ExcavatingRaycastProjectile");
        java.util.function.BiConsumer<Location,Location> callback=(from,to)->calls.add(new Location[]{from.clone(),to.clone()});
        CustomProjectile ray=(CustomProjectile)type.getConstructor(java.util.function.BiConsumer.class).newInstance(callback);
        Location start=new Location(world,-2,64,-3);
        ray.shoot(manager,start,new Vector(3,0,0),null);manager.tick();manager.tick();
        check(calls.size()==1,"callback exactly once");
        check(calls.getFirst()[0].equals(start) && Math.abs(calls.getFirst()[1].getX()-62)<1e-8,"full actual 64-block capped ray");
        check(ray.getDistanceTravelled()==64 && !ray.isActive() && queries[0]==0,"range and collision-independent removal");
        clipped[0]=true;calls.clear();
        ray=(CustomProjectile)type.getConstructor(java.util.function.BiConsumer.class).newInstance(callback);
        ray.shoot(manager,new Location(world,0,64,0),new Vector(1,0,0),null);manager.tick();manager.tick();
        check(calls.size()==1 && calls.getFirst()[1].getX()<16 && calls.getFirst()[1].getX()>15.99,"callback uses loaded-chunk actual travelled cap, not requested endpoint");
        manager.close();
        System.out.println("Excavating ray boundary checks passed: 64-block cap, ignore collision, once-only actual travel callback");
    }
}
