package me.simoncrafter.mCCodeCampLibrary.input.activation;

import me.simoncrafter.mCCodeCampLibrary.input.activation.event.ProjectileTargetHitEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.*;
import me.simoncrafter.mCCodeCampLibrary.utility.Cooldown;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.plugin.*;
import org.spongepowered.configurate.BasicConfigurationNode;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

/** Production target code with Bukkit API doubles, not a live server test. */
public class ProjectileTargetHarness {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    @SuppressWarnings("unchecked") static <T> T proxy(Class<T> type, InvocationHandler h) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (p,m,a) -> switch(m.getName()) {
            case "hashCode" -> System.identityHashCode(p); case "equals" -> p == a[0];
            case "toString" -> type.getSimpleName(); default -> h.invoke(p,m,a);
        });
    }
    static class Registry extends BlockMarkerRegistry {
        int saves; Registry(Plugin p) { super(p); }
        @Override public void saveObject(IBlockRegestryObject o) { saves++; }
    }
    public static void main(String[] args) throws Exception {
        List<Event> events = new ArrayList<>();
        PluginManager pm = proxy(PluginManager.class,(p,m,a)->{if(m.getName().equals("callEvent")) events.add((Event)a[0]); return null;});
        Logger logger = Logger.getLogger("target-test");
        Server server = proxy(Server.class,(p,m,a)->switch(m.getName()) {
            case "getPluginManager" -> pm; case "getLogger" -> logger; case "isPrimaryThread" -> true;
            case "getWorlds" -> List.of(); case "getName", "getVersion", "getBukkitVersion" -> "test"; default -> null;
        });
        try { Bukkit.setServer(server); } catch(NoSuchElementException e) { check(Bukkit.getServer()==server,"server installed"); }
        Plugin plugin = proxy(Plugin.class,(p,m,a)->m.getName().equals("getLogger")?logger:m.getReturnType()==String.class?"target-test":null);
        Registry registry = new Registry(plugin);
        World world = proxy(World.class,(p,m,a)->m.getName().equals("getUID")?new UUID(0,1):null);
        Location loc = new Location(world,10,20,30);
        Block block = proxy(Block.class,(p,m,a)->switch(m.getName()) {
            case "getWorld" -> world; case "getX" -> 10; case "getY" -> 20; case "getZ" -> 30; case "getLocation" -> loc.clone(); default -> null;
        });
        Projectile arrow = proxy(Projectile.class,(p,m,a)->switch(m.getName()) {case "getLocation" -> loc.clone().add(.1,.2,.3); default -> null;});
        var config = BasicConfigurationNode.root();
        ProjectileTarget target = new ProjectileTarget();
        var type = new RegistryObjectType(ProjectileTarget::new,"target");
        target.init(plugin,loc,"bullseye",type,config,UUID.randomUUID(),registry);
        if (args.length > 0 && args[0].equals("--contract-doubles")) {
            var custom = me.simoncrafter.mCCodeCampLibrary.projectile.CustomProjectile.class.getDeclaredConstructor().newInstance();
            var contact = new me.simoncrafter.mCCodeCampLibrary.projectile.ProjectileHit(block,null,new org.bukkit.util.Vector(10,20.5,30.5),BlockFace.NORTH);
            var customEvent = new me.simoncrafter.mCCodeCampLibrary.projectile.event.ProjectileHitEvent(custom,contact);
            target.onCustomProjectileHit(customEvent);
            check(events.size()==1 && ((ProjectileTargetHitEvent)events.getFirst()).getCustomProjectile()==custom,"custom exact-block hit activates with vanilla disabled");
            check(((ProjectileTargetHitEvent)events.getFirst()).getHitPosition().getY()==20.5,"custom collision position preserved");
            customEvent.setCancelled(true); target.onCustomProjectileHit(customEvent);
            check(events.size()==1 && customEvent.isCancelled(),"cancelled custom hit ignored without mutation");
            events.clear();
        }
        var hit = new org.bukkit.event.entity.ProjectileHitEvent(arrow,null,block,BlockFace.NORTH);
        target.onVanillaProjectileHit(hit);
        check(events.isEmpty(),"vanilla opt-in defaults false");
        target.setDetectVanillaProjectiles(true);
        target.onVanillaProjectileHit(hit);
        check(events.size()==1,"vanilla block hit activates");
        var event = (ProjectileTargetHitEvent)events.getFirst();
        check(event.getTarget()==target && event.getID().equals("bullseye") && event.getVanillaProjectile()==arrow && event.getCustomProjectile()==null,"event identity");
        check(event.getShooter()==null && event.getHitFace()==BlockFace.NORTH,"optional shooter and face");
        target.onVanillaProjectileHit(new org.bukkit.event.entity.ProjectileHitEvent(arrow,(Entity)null,(Block)null,(BlockFace)null));
        Block neighbor = proxy(Block.class,(p,m,a)->switch(m.getName()) { case "getWorld" -> world; case "getX" -> 11; case "getY" -> 20; case "getZ" -> 30; default -> null; });
        target.onVanillaProjectileHit(new org.bukkit.event.entity.ProjectileHitEvent(arrow,null,neighbor,BlockFace.NORTH));
        check(events.size()==1,"entity/no-block hits ignored");
        hit.setCancelled(true); target.onVanillaProjectileHit(hit);
        check(events.size()==1,"cancelled hit ignored even directly invoked"); hit.setCancelled(false);
        target.setCooldown(2); target.onVanillaProjectileHit(hit); target.onVanillaProjectileHit(hit);
        check(events.size()==2,"cooldown suppresses repeated hit");
        Cooldown.tickAll(); Cooldown.tickAll(); target.onVanillaProjectileHit(hit);
        check(events.size()==3,"tick expiry allows another hit");
        check(config.node("cooldown").getInt()==2 && config.node("detectVanillaProjectiles").getBoolean() && registry.saves==2,"setters persist");
        Location copy = event.getHitPosition(); copy.setX(999);
        check(event.getHitPosition().getX()!=999,"event position defensive copy");
        Location targetCopy = target.getLocation(); targetCopy.setX(999);
        check(target.getLocation().getBlockX()==10,"target location defensive copy");
        boolean rejected = false;
        try { target.setCooldown(-1); } catch(IllegalArgumentException expected) { rejected=true; }
        check(rejected && target.getCooldown()==2,"negative duration rejected without mutation");
        target.setBlockData(null); target.setCooldownBlockdata(null);
        check(target.getBlockData()==null && target.getCooldownBlockdata()==null && registry.saves==4,"optional block states clear and save");
        for(String method:List.of("onVanillaProjectileHit","onCustomProjectileHit")) {
            var annotation = Arrays.stream(ProjectileTarget.class.getMethods()).filter(m->m.getName().equals(method)).findFirst().orElseThrow().getAnnotation(EventHandler.class);
            check(annotation.priority()==EventPriority.MONITOR && annotation.ignoreCancelled(),"monitor observes accepted hits only");
        }
        Cooldown old = target.getCooldownInstance(); target.destroy(); target.destroy();
        target.onVanillaProjectileHit(hit); check(events.size()==3,"destroyed target inert");
        old.start(); check(!old.isOnCooldown(),"cooldown disposed");
        target.init(plugin,loc,"again",type,config,UUID.randomUUID(),registry);
        target.onVanillaProjectileHit(hit); check(events.size()==4,"reinit works");
        target.destroy(); Cooldown.disposeAll();
        System.out.println("PASS projectile target: opt-in, accepted block hits, event data, cancellation, cooldown, persistence, monitor handlers, cleanup/reinit");
    }
}
