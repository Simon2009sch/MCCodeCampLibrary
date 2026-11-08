package me.simoncrafter.mCCodeCampLibrary.projectile.effect;

import me.simoncrafter.mCCodeCampLibrary.projectile.effect.backup.TerrainBackups;
import org.bukkit.*;
import org.bukkit.plugin.*;
import org.bukkit.scheduler.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/** Real coordinator with controlled backup futures and Bukkit scheduling/world boundary doubles. */
public class ProjectileEffectsHarness {
    static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    @SuppressWarnings("unchecked") static <T> T proxy(Class<T> type,InvocationHandler action) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->{
            if(m.getName().equals("hashCode")) return System.identityHashCode(p);
            if(m.getName().equals("equals")) return p==a[0];
            if(m.getName().equals("toString")) return type.getSimpleName();
            return action.invoke(p,m,a);
        });
    }
    static final class Backups implements ProjectileEffects.BackupAccess {
        int captures,restores; boolean closed;
        CompletableFuture<Long> capture=new CompletableFuture<>();
        CompletableFuture<Integer> restore=new CompletableFuture<>();
        public CompletableFuture<Long> capture(World world,TerrainBackups.Bounds bounds){captures++;return capture;}
        public CompletableFuture<Integer> restoreAll(){restores++;return restore;}
        public int count(){return captures;}
        public boolean busy(){return false;}
        public void close(){closed=true;}
    }
    public static void main(String[] args) {
        Logger logger=Logger.getLogger("effects-coordinator-test");
        int[] cancelledTasks={0};
        BukkitTask task=proxy(BukkitTask.class,(p,m,a)->{if(m.getName().equals("cancel"))cancelledTasks[0]++;return null;});
        BukkitScheduler scheduler=proxy(BukkitScheduler.class,(p,m,a)->m.getName().equals("runTaskTimer")?task:null);
        PluginManager pm=proxy(PluginManager.class,(p,m,a)->null);
        Server server=proxy(Server.class,(p,m,a)->switch(m.getName()){
            case "isPrimaryThread"->true;case "getScheduler"->scheduler;case "getPluginManager"->pm;case "getLogger"->logger;
            case "getOnlinePlayers"->List.of();
            case "getName","getVersion","getBukkitVersion"->"test";default->null;
        });
        try{Bukkit.setServer(server);}catch(NoSuchElementException expected){check(Bukkit.getServer()==server,"API setup");}
        Plugin plugin=proxy(Plugin.class,(p,m,a)->switch(m.getName()){case "isEnabled"->true;case "getName","namespace"->"test";case "getLogger"->logger;default->null;});
        String[] name={"testworld"}; UUID worldID=UUID.randomUUID();
        World world=proxy(World.class,(p,m,a)->switch(m.getName()){
            case "getName"->name[0];case "getUID"->worldID;case "getMinHeight"->-64;case "getMaxHeight"->320;
            case "getPlayers"->List.of();case "isChunkLoaded"->true;default->null;
        });
        Location at=new Location(world,-1368,70,3350);
        Backups backups=new Backups();
        ProjectileEffects effects=new ProjectileEffects(plugin,backups);
        effects.beginImpact(at,null,false);
        check(backups.captures==1 && effects.status().contains("phase=BACKUP"),"mutation waits behind backup future");
        backups.capture.completeExceptionally(new IllegalStateException("expected backup failure"));
        check(effects.status().contains("phase=IDLE") && effects.status().contains("explosions=0"),"failed backup prevents impact damage");
        backups.capture=new CompletableFuture<>();effects.beginImpact(at,null,false);backups.capture.complete(7L);
        check(effects.status().contains("phase=IMPACT"),"successful durable backup starts impact");
        check(effects.restore(null) && backups.restores==1,"restore stops effects before requesting reverse paste");
        check(effects.status().contains("phase=RESTORING"),"restoring state prevents new operations");
        effects.beginImpact(at,null,false);check(backups.captures==2,"new impact rejected while restore runs");
        backups.restore.complete(2);check(effects.status().contains("phase=IDLE") && !effects.status().contains("live=true"),"restoration completes disarmed");
        effects.close();check(backups.closed,"owned backup worker closes");
        name[0]="world";backups=new Backups();effects=new ProjectileEffects(plugin,backups);
        effects.beginImpact(at,null,false);check(backups.captures==0 && effects.status().contains("phase=IMPACT"),"normal world is visual only");effects.close();
        name[0]="testworld";backups=new Backups();effects=new ProjectileEffects(plugin,backups);
        effects.beginImpact(at,null,false);effects.close();backups.capture.complete(8L);
        check(effects.status().contains("phase=CLOSED"),"late capture cannot restart closed coordinator");
        check(cancelledTasks[0]==3,"each coordinator releases its one tick task");
        System.out.println("Projectile effects gate checks passed: backup-before-damage, failure, restore lock, world policy, late callback and cleanup");
    }
}
