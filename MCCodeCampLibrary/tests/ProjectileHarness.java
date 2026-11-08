package me.simoncrafter.mCCodeCampLibrary.projectile;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

/** Production projectile control flow exercised with a minimal Bukkit boundary. */
public class ProjectileHarness {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    @SuppressWarnings("unchecked") static <T> T proxy(Class<T> type, InvocationHandler action) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p,m,a) -> {
            if (m.getName().equals("hashCode")) return System.identityHashCode(p);
            if (m.getName().equals("equals")) return p == a[0];
            if (m.getName().equals("toString")) return type.getSimpleName();
            return action.invoke(p,m,a);
        });
    }
    static class Probe extends CustomProjectile {
        int ticks, hits, removed;
        Probe() { super(ProjectileMovement.FIREBALL_LIKE); }
        @Override protected void onTick() { ticks++; }
        @Override protected void onHit(ProjectileHit hit) { hits++; super.onHit(hit); }
        @Override protected void onRemove() { removed++; }
    }
    public static void main(String[] args) {
        boolean[] cancel = {false}, loaded = {true}, primary = {true}, hitBlock = {false}, hitEntity = {false};
        List<Event> events = new ArrayList<>();
        List<java.util.function.Consumer<me.simoncrafter.mCCodeCampLibrary.projectile.event.ProjectileHitEvent>> impactActions = new ArrayList<>();
        PluginManager pm = proxy(PluginManager.class, (p,m,a) -> {
            if (m.getName().equals("registerEvents")) {
                me.simoncrafter.mCCodeCampLibrary.projectile.event.ProjectileHitEvent.getHandlerList().register(
                    new org.bukkit.plugin.RegisteredListener((org.bukkit.event.Listener)a[0],(listener,event) -> {},org.bukkit.event.EventPriority.NORMAL,(Plugin)a[1],false));
            }
            if (m.getName().equals("callEvent")) { events.add((Event)a[0]); if (cancel[0] && a[0] instanceof me.simoncrafter.mCCodeCampLibrary.projectile.event.ProjectileHitEvent e) e.setCancelled(true); }
            if (m.getName().equals("callEvent") && a[0] instanceof me.simoncrafter.mCCodeCampLibrary.projectile.event.ProjectileHitEvent e) {
                for (var action : List.copyOf(impactActions)) action.accept(e);
            }
            return null;
        });
        Logger logger = Logger.getLogger("projectile-test");
        Server server = proxy(Server.class, (p,m,a) -> switch(m.getName()) {
            case "isPrimaryThread" -> primary[0]; case "getPluginManager" -> pm;
            case "getName", "getVersion", "getBukkitVersion" -> "test"; case "getLogger" -> logger; default -> null;
        });
        try { Bukkit.setServer(server); } catch (NoSuchElementException expected) { check(Bukkit.getServer() == server, "API-only setup"); }
        Plugin plugin = proxy(Plugin.class, (p,m,a) -> switch(m.getName()) { case "getName" -> "projectile-test"; case "getLogger" -> logger; case "isEnabled" -> true; default -> null; });
        Block block = proxy(Block.class, (p,m,a) -> switch(m.getName()) { case "getX" -> 2; case "getY", "getZ" -> 0; default -> null; });
        Entity entity = proxy(Entity.class, (p,m,a) -> switch(m.getName()) { case "isValid" -> true; case "isDead" -> false; default -> null; });
        double[] traced = {0};
        World world = proxy(World.class, (p,m,a) -> switch(m.getName()) {
            case "isChunkLoaded" -> loaded[0] && (int)a[0] < 1;
            case "getMinHeight" -> -64; case "getMaxHeight" -> 320;
            case "rayTraceBlocks" -> {
                check(a.length==6,"Paper block collision predicate overload is used");
                @SuppressWarnings("unchecked") var filter = (java.util.function.Predicate<Block>)a[5];
                Location from = (Location)a[0]; double distance = (double)a[2]; traced[0] = Math.max(traced[0],distance);
                yield hitBlock[0] && filter.test(block) && from.getX() <= 2 && from.getX()+distance >= 2 ? new org.bukkit.util.RayTraceResult(new Vector(2,0,0),block,BlockFace.WEST) : null;
            }
            case "rayTraceEntities" -> {
                Location from = (Location)a[0]; double distance = (double)a[2];
                @SuppressWarnings("unchecked") var filter = (java.util.function.Predicate<Entity>)a[4];
                yield hitEntity[0] && filter.test(entity) && from.getX() <= 1 && from.getX()+distance >= 1 ? new org.bukkit.util.RayTraceResult(new Vector(1,0,0),entity,BlockFace.WEST) : null;
            }
            default -> null;
        });
        Location start = new Location(world,0,0,0);
        ProjectileManager manager = new ProjectileManager(plugin);
        Probe p = new Probe(); p.shoot(manager,start,new Vector(1,0,0),null); manager.tick();
        check(p.ticks == 1 && p.getLocation().getX() == 1,"tick and move");
        p.getVelocity().setX(99); p.getLocation().setX(99); check(p.getVelocity().getX()==1 && p.getLocation().getX()==1,"defensive copies");
        p.remove(); p.remove(); check(p.removed==1 && manager.size()==0,"idempotent removal");
        p = new Probe(); p.setMaxTicks(2); p.shoot(manager,start,new Vector(),null); manager.tick(); manager.tick();
        check(!p.isActive() && p.ticks==2,"stationary expiration");
        hitBlock[0]=true;
        p = new Probe(); p.shoot(manager,start,new Vector(5,0,0),null); manager.tick();
        check(p.hits==1 && !p.isActive() && p.getLocation().getX()==2,"fast block sweep stops at hit");
        var event = (me.simoncrafter.mCCodeCampLibrary.projectile.event.ProjectileHitEvent)events.getLast();
        check(event.getHit().block()==block && event.getHit().face()==BlockFace.WEST,"block face");
        cancel[0]=true; p = new Probe(); p.shoot(manager,start,new Vector(5,0,0),null); manager.tick();
        check(p.hits==0 && p.isActive() && p.getLocation().getX()==5,"cancelled hit passes through without behavior"); p.remove(); cancel[0]=false;
        impactActions.add(e -> { e.getProjectile().setMaxDistance(3); e.setCancelled(true); });
        p = new Probe(); p.shoot(manager,start,new Vector(5,0,0),null); manager.tick();
        check(!p.isActive() && p.getLocation().getX()==3,"cancel callback immediately constrains remaining range"); impactActions.clear();
        hitEntity[0]=true; p = new Probe(); p.shoot(manager,start,new Vector(5,0,0),null); manager.tick();
        check(p.hits==1 && p.getLocation().getX()==1,"entity before wall");
        hitBlock[0]=false; hitEntity[0]=false;
        p = new Probe(); p.setMovement(ProjectileMovement.ARROW_LIKE); p.shoot(manager,start,new Vector(1,0,0),null); manager.tick();
        check(p.getLocation().getX()==1 && p.getVelocity().getY()==-.05,"arrow physics"); p.remove();
        p = new Probe(); p.setMovement(ProjectileMovement.SNOWBALL_LIKE); p.shoot(manager,start,new Vector(1,0,0),null); manager.tick();
        check(p.getVelocity().getY()==-.03,"snowball physics"); p.remove();
        p = new Probe(); p.setMovement(ProjectileMovement.RAYCAST); p.setMaxDistance(10); p.shoot(manager,start,new Vector(1,0,0),null); manager.tick();
        check(!p.isActive() && p.getLocation().getX()==10,"finite instantaneous raycast");
        p = new Probe(); p.setMaxDistance(4); p.shoot(manager,start,new Vector(9,0,0),null); manager.tick();
        check(!p.isActive() && p.getLocation().getX()==4,"range cap");
        traced[0]=0; p = new Probe(); p.shoot(manager,start,new Vector(40,0,0),null); manager.tick();
        check(!p.isActive() && p.getLocation().getX()<16 && traced[0]<16,"no unloaded chunk trace");
        loaded[0]=false; p = new Probe(); p.shoot(manager,start,new Vector(1,0,0),null); manager.tick(); check(!p.isActive(),"unloaded start disposes"); loaded[0]=true;
        CustomProjectile noFace = new CustomProjectile() { @Override protected ProjectileHit hit(Location from, Vector step) { return new ProjectileHit(block,null,from.toVector(),null); } };
        noFace.shoot(manager,start,new Vector(1,0,0),null); manager.tick();
        check(((me.simoncrafter.mCCodeCampLibrary.projectile.event.ProjectileHitEvent)events.getLast()).getHit().face()==null,"null face supported");
        try { new ProjectileHit(null,null,new Vector(),null); throw new AssertionError("missing target accepted"); } catch(IllegalArgumentException expected) { }
        try { new ProjectileHit(block,entity,new Vector(),null); throw new AssertionError("ambiguous target accepted"); } catch(IllegalArgumentException expected) { }
        int[] particles = {0};
        World visualWorld = proxy(World.class,(obj,m,a) -> {
            if (m.getName().equals("spawnParticle")) { particles[0]++; return null; }
            if (m.getName().equals("isChunkLoaded")) return true;
            if (m.getName().equals("getMinHeight")) return -64;
            if (m.getName().equals("getMaxHeight")) return 320;
            return null;
        });
        ParticleProjectile trail = new ParticleProjectile(ProjectileMovement.FIREBALL_LIKE,Particle.END_ROD);
        trail.shoot(manager,new Location(visualWorld,0,0,0),new Vector(2,0,0),null); manager.tick();
        check(particles[0] > 1,"particle trail spans fast movement"); trail.remove();
        check(new ArrowProjectile().getMovement()==ProjectileMovement.ARROW_LIKE,"premade arrow");
        check(new SnowballProjectile().getMovement()==ProjectileMovement.SNOWBALL_LIKE,"premade snowball");
        check(new FireballProjectile().getMovement()==ProjectileMovement.FIREBALL_LIKE,"premade fireball");
        check(new RaycastProjectile().getMovement()==ProjectileMovement.RAYCAST,"premade raycast");
        org.bukkit.event.Listener owned = new org.bukkit.event.Listener() {};
        CustomProjectile listening = new CustomProjectile() { @Override protected void onShoot() { listen(owned); listen(owned); } };
        listening.shoot(manager,start,new Vector(),null);
        var handlers = me.simoncrafter.mCCodeCampLibrary.projectile.event.ProjectileHitEvent.getHandlerList();
        check(handlers.getRegisteredListeners().length==1,"owned listener registered once");
        listening.remove(); check(handlers.getRegisteredListeners().length==0,"owned listener unregistered on removal");
        CustomProjectile failing = new CustomProjectile() { @Override protected void onTick() { throw new IllegalStateException("expected test failure"); } };
        failing.shoot(manager,start,new Vector(),null);
        Probe survivor = new Probe(); survivor.shoot(manager,start,new Vector(),null);
        logger.setLevel(java.util.logging.Level.OFF); manager.tick(); logger.setLevel(java.util.logging.Level.INFO);
        check(!failing.isActive() && survivor.ticks==1,"failed hook isolated from other projectiles"); survivor.remove();
        Probe nested = new Probe();
        nested.whenTick(projectile -> { if (projectile.getAge()==1) manager.tick(); });
        nested.shoot(manager,start,new Vector(1,0,0),null); manager.tick();
        check(nested.ticks==1 && nested.getLocation().getX()==1,"nested manager tick does not update recursively"); nested.remove();
        int[] movementErrors = {0};
        logger.addHandler(new java.util.logging.Handler() {
            @Override public void publish(java.util.logging.LogRecord record) { if (record.getMessage().equals("Removing failed custom projectile")) movementErrors[0]++; }
            @Override public void flush() {} @Override public void close() {}
        });
        CustomProjectile selfRemoving = new CustomProjectile() { @Override protected Vector move() { remove(); return null; } };
        selfRemoving.shoot(manager,start,new Vector(),null);
        logger.setUseParentHandlers(false); manager.tick(); logger.setUseParentHandlers(true);
        check(!selfRemoving.isActive() && movementErrors[0]==0,"movement removal stops before validating its result");
        Probe unloadProbe = new Probe(); unloadProbe.shoot(manager,start,new Vector(),null);
        manager.onWorldUnload(new org.bukkit.event.world.WorldUnloadEvent(world));
        check(!unloadProbe.isActive(),"world unload immediately releases projectile");
        p = new Probe(); p.shoot(manager,start,new Vector(),null); manager.close(); check(!p.isActive() && manager.size()==0,"shutdown cleanup");
        try {
            var pluginField = me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib.class.getDeclaredField("plugin");
            pluginField.setAccessible(true); pluginField.set(null,plugin);
            me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib.onDisable();
            try {
                me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib.init(plugin);
                throw new AssertionError("same-classloader reinitialization must explicitly fail, not silently stay disabled");
            } catch (IllegalStateException expected) { check(expected.getMessage().contains("classloader"),"clear lifecycle contract"); }
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
        primary[0]=false; try { new ProjectileManager(plugin).tick(); throw new AssertionError("async accepted"); } catch(IllegalStateException expected) { }
        System.out.println("Projectile boundary checks passed");
    }
}
