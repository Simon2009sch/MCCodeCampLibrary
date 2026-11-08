package me.simoncrafter.mCCodeCampLibrary.projectile.effect;

import me.simoncrafter.mCCodeCampLibrary.projectile.*;
import me.simoncrafter.mCCodeCampLibrary.projectile.effect.backup.TerrainBackups;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/** Owns backup-gated terrain effects. No terrain mutation is allowed before a durable snapshot. */
public final class ProjectileEffects implements Listener, AutoCloseable {
    interface BackupAccess extends AutoCloseable {
        CompletableFuture<Long> capture(World world, TerrainBackups.Bounds bounds);
        CompletableFuture<Integer> restoreAll();
        int count(); boolean busy(); void close();
    }
    private enum Phase { IDLE, FALLING, BACKUP, IMPACT, RAY, RESTORING, CLOSED }
    private record Cell(int x,int y,int z) { }
    private final Plugin plugin;
    private final BackupAccess backups;
    private final DestructionPolicy policy = new DestructionPolicy();
    private MeteorVisuals visuals;
    private ImpactFlash flash;
    private final Set<CustomProjectile> owned = Collections.newSetFromMap(new IdentityHashMap<>());
    private final ArrayDeque<Cell> rayCells = new ArrayDeque<>();
    private final BukkitTask task;
    private SurfaceImpact surface;
    private MeteorProjectile falling;
    private World activeWorld;
    private Phase phase = Phase.IDLE;
    private long generation, lastChanged, lastExplosions, lastBurned;
    private int pendingBackups;
    private boolean closed;

    public ProjectileEffects(Plugin plugin) { this(plugin, openBackups(plugin)); }
    ProjectileEffects(Plugin plugin, BackupAccess backups) {
        mainThread(); this.plugin=Objects.requireNonNull(plugin);this.backups=backups;
        visuals=new MeteorVisuals(plugin);flash=new ImpactFlash(plugin);
        Bukkit.getPluginManager().registerEvents(this,plugin);
        task=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1,1);
    }
    private static BackupAccess openBackups(Plugin plugin) {
        try {
            TerrainBackups service=new TerrainBackups(plugin,plugin.getDataFolder().toPath().resolve("terrain-backups"));
            return new BackupAccess() {
                public CompletableFuture<Long> capture(World world,TerrainBackups.Bounds bounds) {return service.capture(world,bounds).thenApply(TerrainBackups.Backup::sequence);}
                public CompletableFuture<Integer> restoreAll(){return service.restoreAll();}
                public int count(){return service.backupCount();}
                public boolean busy(){return service.isBusy();}
                public void close(){service.close();}
            };
        } catch (RuntimeException | LinkageError failure) {
            plugin.getLogger().log(Level.WARNING,"Terrain backup service unavailable: destructive effects disabled; visuals remain available",failure);
            return null;
        }
    }
    private static void mainThread(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Effects require the server thread");}
    private boolean idle(Entity source) {
        if(closed||phase!=Phase.IDLE||pendingBackups>0||(backups!=null&&backups.busy())) {
            tell(source,"An effect or backup/restore is already running; wait before trying again.");return false;
        }
        return true;
    }
    /** Creates a normal test-wand projectile; only its accepted impact can trigger the next effect. */
    public CustomProjectile createProjectile(ProjectileMovement mode, Entity shooter) {
        mainThread();if(closed)throw new IllegalStateException("Effects closed");
        CustomProjectile projectile;
        switch(mode) {
            case ARROW_LIKE -> projectile=new ArrowProjectile(0);
            case SNOWBALL_LIKE -> projectile=new SnowballProjectile();
            case FIREBALL_LIKE -> {
                FireballProjectile bolt=new FireballProjectile(0);
                bolt.whenHit(hit->summonMeteor(bolt.getLocation(),shooter,false));projectile=bolt;
            }
            case RAYCAST -> projectile=new ExcavatingRaycastProjectile((from,to)->excavate(from,to,shooter,false));
            default -> throw new IllegalArgumentException("Unknown projectile movement");
        }
        owned.add(projectile);return projectile;
    }
    public boolean summonMeteor(Location target, Entity shooter, boolean live) {
        mainThread();if(!idle(shooter))return false;
        Objects.requireNonNull(target.getWorld(),"world");target.checkFinite();
        if(live&&!policy.permits(target.getWorld().getName(),target.getX(),target.getZ(),true)) {
            tell(shooter,"Live test refused: only the explicitly approved world/epicenter is allowed.");return false;
        }
        phase=Phase.FALLING;activeWorld=target.getWorld();long token=generation;
        MeteorProjectile meteor=new MeteorProjectile(at->{if(!closed&&token==generation)beginImpact(at,shooter,live);});
        meteor.setEntityFilter(entity->false);meteor.setMaxTicks(100);meteor.setMaxDistance(180);
        Location top=target.clone();top.setY(Math.min(target.getWorld().getMaxHeight()-2,target.getY()+70));
        falling=meteor;owned.add(meteor);
        try {meteor.shoot(MCCodeCampLib.getProjectileManager(),top,new Vector(0,-3,0),shooter);}
        catch(RuntimeException failure){phase=Phase.IDLE;owned.remove(meteor);throw failure;}
        log("meteor descending at "+position(target));return true;
    }
    /** Shared ground-impact seam; package-visible for executable backup-gate tests. */
    void beginImpact(Location at, Entity shooter, boolean live) {
        mainThread();if(closed||phase==Phase.RESTORING||(phase!=Phase.IDLE&&phase!=Phase.FALLING))return;
        Location center=at.clone();activeWorld=center.getWorld();
        flash.flash(center); // The screen flash is visual and happens at physical impact, not after disk I/O.
        boolean destructive=policy.permits(activeWorld.getName(),center.getX(),center.getZ(),live);
        if(!destructive||backups==null) {
            phase=Phase.IMPACT;visuals.startImpact(center,false,(point,radius)->{});
            tell(shooter,"Meteor visuals only: terrain/entity damage requires testworld and a working FAWE backup service.");return;
        }
        TerrainBackups.Bounds bounds=new TerrainBackups.Bounds(center.getBlockX()-90,activeWorld.getMinHeight(),center.getBlockZ()-90,
                center.getBlockX()+90,activeWorld.getMaxHeight()-1,center.getBlockZ()+90);
        captureThen(center,bounds,shooter,()->{
            surface=new SurfaceImpact(center);phase=Phase.IMPACT;
            visuals.startImpact(center,true,surface::enqueueBurn);
            log("meteor backup committed; destructive impact starting at "+position(center));
        });
    }
    private void captureThen(Location at,TerrainBackups.Bounds bounds,Entity source,Runnable mutation) {
        phase=Phase.BACKUP;pendingBackups++;long token=generation;
        log("saving original terrain before damage at "+position(at));
        try {
            backups.capture(at.getWorld(),bounds).whenComplete((sequence,error)->onMain(()->{
                if(closed)return;
                pendingBackups=Math.max(0,pendingBackups-1);
                if(token!=generation)return;
                if(error!=null){phase=Phase.IDLE;fail(source,"Backup failed; no destructive job was started",error);return;}
                log("durable backup #"+sequence+" committed");
                try {mutation.run();}catch(RuntimeException failure){stopActive(true);phase=Phase.IDLE;fail(source,"Effect failed; its backup is retained",failure);}
            }));
        } catch(RuntimeException failure) {
            pendingBackups--;phase=Phase.IDLE;fail(source,"Backup failed; no destructive job was started",failure);
        }
    }
    public boolean excavate(Location from,Location to,Entity source,boolean live) {
        mainThread();if(!idle(source))return false;
        if(from.getWorld()==null||from.getWorld()!=to.getWorld())throw new IllegalArgumentException("Ray endpoints must share a world");
        if(!policy.permits(from.getWorld().getName(),from.getX(),from.getZ(),live)||backups==null) {
            tell(source,"Ray visuals only: excavation is restricted to testworld with a working FAWE backup service.");return false;
        }
        Capsule capsule=new Capsule(from.toVector(),to.toVector(),4);
        int minY=Math.max(from.getWorld().getMinHeight(),capsule.minY()),maxY=Math.min(from.getWorld().getMaxHeight()-1,capsule.maxY());
        if(minY>maxY)return false;
        activeWorld=from.getWorld();
        TerrainBackups.Bounds bounds=new TerrainBackups.Bounds(capsule.minX(),minY,capsule.minZ(),capsule.maxX(),maxY,capsule.maxZ());
        captureThen(from,bounds,source,()->{
            for(int x=bounds.minX();x<=bounds.maxX();x++)for(int y=bounds.minY();y<=bounds.maxY();y++)for(int z=bounds.minZ();z<=bounds.maxZ();z++)
                if(capsule.contains(x+.5,y+.5,z+.5))rayCells.add(new Cell(x,y,z));
            lastChanged=0;phase=Phase.RAY;log("ray backup committed; clearing "+rayCells.size()+" capsule cells");
        });return true;
    }
    public boolean restore(Entity source) {
        mainThread();
        if(closed||phase==Phase.RESTORING||pendingBackups>0||backups==null||backups.busy()) {
            tell(source,"Restore unavailable while a backup/restore is pending, or without FAWE.");return false;
        }
        stopActive(true);policy.disarmLiveTest();phase=Phase.RESTORING;long token=generation;
        log("restoring all backups newest first");
        try {
            backups.restoreAll().whenComplete((count,error)->onMain(()->{
                if(closed||token!=generation)return;
                phase=Phase.IDLE;
                if(error!=null)fail(source,"Restore stopped; failed/unapplied backups remain on disk",error);
                else {tell(source,"Restored "+count+" terrain backups, newest first; applied backups deleted.");log("restore complete: "+count+" applied backups deleted");}
            }));
        } catch(RuntimeException failure){phase=Phase.IDLE;fail(source,"Restore could not start; backups retained",failure);return false;}
        return true;
    }
    /** Console integration only: fixed, explicitly authorized test center. This state is never persisted. */
    public boolean liveMeteor() {
        mainThread();World world=Bukkit.getWorld("world");if(world==null||!idle(null))return false;
        policy.armLiveTest();return summonMeteor(new Location(world,-1368,70,3350),null,true);
    }
    public boolean liveRay() {
        mainThread();World world=Bukkit.getWorld("world");if(world==null||!idle(null))return false;
        policy.armLiveTest();return excavate(new Location(world,-1368,73,3350),new Location(world,-1304,73,3350),null,true);
    }
    public void disarmLiveTest() {
        mainThread();policy.disarmLiveTest();
        if(phase!=Phase.RESTORING){stopActive(true);phase=Phase.IDLE;}
        log("live permit disarmed; normal damage world is testworld");
    }
    public String status() {
        return "phase="+phase+", destructive-world=testworld, live="+policy.liveArmed()+", backups="+(backups==null?"unavailable":backups.count())
                +", pending="+pendingBackups+", explosions="+(surface==null?lastExplosions:surface.explosions())
                +", changedBlocks="+(surface==null?lastChanged:surface.changedBlocks())+", burnedEntities="+(surface==null?lastBurned:surface.burnedEntities());
    }
    public void tick() {
        mainThread();if(closed)return;
        try {
            flash.tick();visuals.tick();
            if(surface!=null)surface.tick();
            if(phase==Phase.RAY) {
                for(int n=0;n<512&&!rayCells.isEmpty();n++) {
                    Cell cell=rayCells.removeFirst();
                    if(!activeWorld.isChunkLoaded(cell.x>>4,cell.z>>4))throw new IllegalStateException("Ray chunk unloaded; backup retained for restore");
                    Block block=activeWorld.getBlockAt(cell.x,cell.y,cell.z);
                    if(!block.getType().isAir()){block.setType(Material.AIR,false);lastChanged++;}
                }
                if(rayCells.isEmpty()){phase=Phase.IDLE;log("ray excavation complete, changedBlocks="+lastChanged);}
            }
            owned.removeIf(projectile->!projectile.isActive());
            if(phase==Phase.FALLING&&falling!=null&&!falling.isActive()) {phase=Phase.IDLE;log("meteor ended without an accepted ground impact");}
            if(phase==Phase.IMPACT&&!visuals.isActive()&&(surface==null||surface.isIdle())) {
                rememberSurface();surface=null;phase=Phase.IDLE;log("impact complete: "+status());
            }
        } catch(RuntimeException failure) {
            stopActive(true);phase=Phase.IDLE;fail(null,"Effect stopped; use restore if terrain had changed",failure);
        }
    }
    @EventHandler public void onBurstDamage(EntityDamageByEntityEvent event) {
        if(event.getDamager() instanceof Snowball snowball&&Boolean.TRUE.equals(snowball.getPersistentDataContainer().get(SnowballProjectile.BURST_KEY,PersistentDataType.BOOLEAN))) {
            Location at=event.getEntity().getLocation();
            if(!policy.permitsEntityDamage(at.getWorld().getName(),at.getX(),at.getZ()))event.setCancelled(true);
        }
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void onWorldUnload(WorldUnloadEvent event) {
        if(event.getWorld()==activeWorld){stopActive(true);policy.disarmLiveTest();phase=Phase.IDLE;}
    }
    private void rememberSurface(){if(surface!=null){lastChanged=surface.changedBlocks();lastExplosions=surface.explosions();lastBurned=surface.burnedEntities();}}
    private void stopActive(boolean recreate) {
        generation++;
        for(CustomProjectile projectile:List.copyOf(owned))try{projectile.remove();}catch(RuntimeException failure){plugin.getLogger().log(Level.WARNING,"Projectile cleanup failed",failure);}
        owned.clear();falling=null;rayCells.clear();rememberSurface();if(surface!=null)surface.cancel();surface=null;
        visuals.close();flash.close();
        if(recreate&&!closed){visuals=new MeteorVisuals(plugin);flash=new ImpactFlash(plugin);}
    }
    @Override public void close() {
        mainThread();if(closed)return;closed=true;stopActive(false);policy.disarmLiveTest();phase=Phase.CLOSED;
        task.cancel();HandlerList.unregisterAll(this);if(backups!=null)backups.close();
    }
    private void onMain(Runnable action) {
        if(closed||!plugin.isEnabled())return;
        if(Bukkit.isPrimaryThread())action.run();else Bukkit.getScheduler().runTask(plugin,()->{if(!closed&&plugin.isEnabled())action.run();});
    }
    private void tell(Entity source,String text){if(source instanceof Player player)player.sendMessage(Component.text(text,NamedTextColor.YELLOW));else log(text);}
    private void fail(Entity source,String message,Throwable error){tell(source,message);plugin.getLogger().log(Level.WARNING,message,error);}
    private void log(String message){plugin.getLogger().info("[ProjectileEffects] "+message);}
    private static String position(Location at){return at.getWorld().getName()+" "+at.getBlockX()+","+at.getBlockY()+","+at.getBlockZ();}
}
