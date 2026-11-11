package me.simoncrafter.mCCodeCampLibrary.projectile.effect;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.random.RandomGenerator;

/** Synchronous, externally ticked destruction job. Caller must obtain permission and complete
 * a full-height +/-90 backup BEFORE constructing. Owns no tasks/listeners; cancel before restore.
 * No native explosions or physics updates for direct edits. Natural fire is not sandboxed.
 */
public final class SurfaceImpact {
    private static final int COLUMN_BUDGET=128;
    private static final int CELL_BUDGET=4096, CRATER_EXTENT=34, CRATER_WIDTH=69;
    private final Location center;
    private final World world;
    private final BooleanSupplier mainThread;
    private final RandomGenerator random;
    private final java.util.function.Predicate<Material> solid, flammable, breakable;

    private final LinkedHashMap<Column,Span> burns=new LinkedHashMap<>();
    private final Set<UUID> burned=new HashSet<>();
    private final List<Column> scars=new ArrayList<>();
    private int craterCell, nextScar, scanY, cellsLeft;
    private long changed, burnedCount;
    private final long noiseSeed;
    private boolean scanningScar;
    private boolean cancelled, scarsPrepared;
    private record Column(int x,int z) {}
    private static final class Span {
        int low,high;
        Span(int low,int high) { this.low=low;this.high=high; }
    }
    public SurfaceImpact(Location center) { this(center, Bukkit::isPrimaryThread, RandomGenerator.getDefault(), Material::isSolid, Material::isFlammable); }
    SurfaceImpact(Location center, BooleanSupplier mainThread, RandomGenerator random,
                  java.util.function.Predicate<Material> solid, java.util.function.Predicate<Material> flammable) {
        this(center, mainThread, random, solid, flammable, material -> material.getHardness() >= 0);
    }
    SurfaceImpact(Location center, BooleanSupplier mainThread, RandomGenerator random,
                  java.util.function.Predicate<Material> solid, java.util.function.Predicate<Material> flammable,
                  java.util.function.Predicate<Material> breakable) {
        this.breakable=Objects.requireNonNull(breakable);
        this.solid=Objects.requireNonNull(solid); this.flammable=Objects.requireNonNull(flammable);
        Objects.requireNonNull(center,"center");
        if(center.getWorld()==null || !finite(center)) throw new IllegalArgumentException("Finite world location required");
        this.center=center.clone(); this.world=center.getWorld();
        this.mainThread=Objects.requireNonNull(mainThread); this.random=Objects.requireNonNull(random);
        noiseSeed=random.nextLong();
    }
    /** Bounded synchronous work, including empty cells and resumable ground/burn scans. */
    public void tick() {
        requireMain(); if(cancelled) return;
        cellsLeft=CELL_BUDGET;
        int budget=COLUMN_BUDGET;
        Iterator<Map.Entry<Column,Span>> it=burns.entrySet().iterator();
        while(budget>0 && cellsLeft>=CELL_BUDGET/2+6 && it.hasNext()) {
            var e=it.next(); Column c=e.getKey();
            budget--;
            if(!loadedArea(c.x+.5,c.z+.5,1)) continue;
            Span span=e.getValue();
            burn(c,span);
            if(span.low>span.high) it.remove();
        }
        // Reserve half the tick for the crater even when burns are busy on later ticks.
        carve();
        if(craterDone()) {
            if(!scarsPrepared) {
                scarsPrepared=true;
                for(int x=-24;x<=24;x++) for(int z=-24;z<=24;z++)
                    if(x*x+z*z<=576) scars.add(new Column(center.getBlockX()+x,center.getBlockZ()+z));
                Collections.shuffle(scars,new Random(random.nextLong()));
            }
            while(budget-->0 && cellsLeft>=5 && nextScar<scars.size()) {
                Column c=scars.get(nextScar);
                if(!loadedArea(c.x+.5,c.z+.5,1)) break;
                Block b=ground(c.x,c.z);
                if(scanningScar) break;
                nextScar++;
                cellsLeft-=4; // exposure lookup and at most three layers, even if ineligible
                if(b!=null && b.getY()+1<world.getMaxHeight()
                        && air(world.getBlockAt(c.x,b.getY()+1,c.z).getType())
                        && stoneLike(b.getType()) && random.nextDouble()<.60) {
                    int depth=1+random.nextInt(3);
                    for(int d=0;d<depth && b.getY()-d>=world.getMinHeight();d++) {
                        Block layer=world.getBlockAt(c.x,b.getY()-d,c.z);
                        if(!stoneLike(layer.getType())) break;
                        change(layer,random.nextBoolean()?Material.MAGMA_BLOCK:Material.BLACKSTONE);
                    }
                }
            }
        }
    }
    private boolean craterDone() { return craterCell==CRATER_WIDTH*CRATER_WIDTH*CRATER_WIDTH; }
    private void carve() {
        while(cellsLeft>0 && !craterDone() && !cancelled) {
            int dx=craterCell/(CRATER_WIDTH*CRATER_WIDTH)-CRATER_EXTENT;
            int dy=(craterCell/CRATER_WIDTH)%CRATER_WIDTH-CRATER_EXTENT;
            int dz=craterCell%CRATER_WIDTH-CRATER_EXTENT;
            int x=center.getBlockX()+dx,y=center.getBlockY()+dy,z=center.getBlockZ()+dz;
            if(!loadedArea(x+.5,z+.5,0)) return; // retry this cell, never force-load
            craterCell++; cellsLeft--; // outside/air cells cost budget too
            if(y<world.getMinHeight() || y>=world.getMaxHeight()) continue;
            // Coordinate noise only affects the shell: guaranteed radius30 core, maximum34.
            long hash=noiseSeed ^ dx*73428767L ^ dy*912931L ^ dz*438289L;
            hash=(hash^(hash>>>33))*0xff51afd7ed558ccdL;
            double radius=32+2*((hash>>>11)*0x1.0p-53*2-1);
            if(dx*dx+dy*dy+dz*dz>radius*radius) continue;
            Block b=world.getBlockAt(x,y,z);
            if(!air(b.getType()) && breakable.test(b.getType())) change(b,Material.AIR);
        }
    }
    /** Queues translated world-space spans, not entity-anchor columns. Radius is clamped to 1..5. */
    public void enqueueBurn(Location translatedPosition, Double radius) {
        requireMain();
        if(cancelled || translatedPosition==null || translatedPosition.getWorld()!=world || !finite(translatedPosition)
                || radius==null || !Double.isFinite(radius)) return;
        double dx=translatedPosition.getX()-center.getX(),dz=translatedPosition.getZ()-center.getZ();
        if(dx*dx+dz*dz>(50.000001*50.000001)) return;
        double r=Math.max(1,Math.min(5,radius));
        if(!loadedArea(translatedPosition.getX(),translatedPosition.getZ(),r+1)) return;
        int low=Math.max(world.getMinHeight(),(int)Math.floor(translatedPosition.getY()-r));
        int high=Math.min(world.getMaxHeight()-1,(int)Math.floor(translatedPosition.getY()+r));
        for(int x=(int)Math.floor(translatedPosition.getX()-r);x<=Math.floor(translatedPosition.getX()+r);x++)
            for(int z=(int)Math.floor(translatedPosition.getZ()-r);z<=Math.floor(translatedPosition.getZ()+r);z++) {
                if(!inside(x,z)) continue;
                Column c=new Column(x,z); Span old=burns.get(c);
                if(old==null) burns.put(c,new Span(low,high));
                else { old.low=Math.min(old.low,low); old.high=Math.max(old.high,high); }
            }
        // Bukkit entity lookup and ignition are synchronous. Items and players intentionally included.
        for(var e:world.getNearbyEntities(translatedPosition,r,r,r)) {
            if(cancelled) break;
            if(e instanceof Display || e instanceof Interaction) continue;
            Location l=e.getLocation();
            if(l.getWorld()==world && inside(l.getBlockX(),l.getBlockZ())
                    && l.distanceSquared(translatedPosition)<=r*r) {
                e.setFireTicks(Math.max(100,e.getFireTicks())); if(burned.add(e.getUniqueId())) burnedCount++;
            }
        }
    }
    private void burn(Column c,Span span) {
        int fires=0;
        while(span.low<=span.high && !cancelled && cellsLeft>=CELL_BUDGET/2+6) {
            int y=span.low++; cellsLeft-=6; // block plus up to five fire neighbours
            Block b=world.getBlockAt(c.x,y,c.z); Material m=b.getType();
            if(foliage(m)) change(b,Material.AIR);
            else if(m==Material.GRASS_BLOCK) change(b,Material.DIRT);
            if(flammable.test(m) && fires<2) {
                int[][] offsets={{0,1,0},{1,0,0},{-1,0,0},{0,0,1},{0,0,-1}};
                for(int[] d:offsets) {
                    int x=c.x+d[0],z=c.z+d[2],fy=y+d[1];
                    if(!inside(x,z) || fy<world.getMinHeight() || fy>=world.getMaxHeight()) continue;
                    Block air=world.getBlockAt(x,fy,z);
                    if(air(air.getType())) { change(air,Material.FIRE); fires++; break; }
                }
            }
        }
    }
    private Block ground(int x,int z) {
        // Ignore foliage AND trunks: height maps alone would detonate on treetops.
        if(!scanningScar) { scanY=world.getMaxHeight()-1; scanningScar=true; }
        while(scanY>=world.getMinHeight() && cellsLeft>5) {
            int y=scanY--; cellsLeft--;
            Block b=world.getBlockAt(x,y,z); Material m=b.getType();
            String n=m.name();
            if(solid.test(m) && !foliage(m) && !n.endsWith("_LOG") && !n.endsWith("_WOOD")
                    && !n.endsWith("_STEM") && !n.endsWith("_HYPHAE")) { scanningScar=false; return b; }
        }
        if(scanY<world.getMinHeight()) scanningScar=false;
        return null;
    }
    private void change(Block b,Material material) {
        if(!cancelled && inside(b.getX(),b.getZ()) && b.getType()!=material) {
            b.setType(material,false); changed++;
        }
    }
    private boolean inside(int x,int z) { return Math.abs((long)x-center.getBlockX())<=90 && Math.abs((long)z-center.getBlockZ())<=90; }
    private boolean loadedArea(double x,double z,double radius) {
        for(int cx=((int)Math.floor(x-radius))>>4;cx<=((int)Math.floor(x+radius))>>4;cx++)
            for(int cz=((int)Math.floor(z-radius))>>4;cz<=((int)Math.floor(z+radius))>>4;cz++)
                if(!world.isChunkLoaded(cx,cz)) return false;
        return true;
    }
    private static boolean foliage(Material m) {
        String n=m.name();
        return n.endsWith("_LEAVES") || n.endsWith("_SAPLING") || n.endsWith("_FLOWER")
                || n.equals("SHORT_GRASS") || n.equals("TALL_GRASS") || n.equals("FERN")
                || n.equals("LARGE_FERN") || n.equals("VINE") || n.equals("DEAD_BUSH")
                || n.equals("DANDELION") || n.equals("POPPY") || n.equals("SNOW");
    }
    private static boolean air(Material m) { return m==Material.AIR || m==Material.CAVE_AIR || m==Material.VOID_AIR; }
    private static boolean stoneLike(Material m) {
        return switch(m) { case STONE,COBBLESTONE,ANDESITE,DIORITE,GRANITE,DEEPSLATE,COBBLED_DEEPSLATE,TUFF -> true; default -> false; };
    }
    private static boolean finite(Location l) { return Double.isFinite(l.getX()) && Double.isFinite(l.getY()) && Double.isFinite(l.getZ()); }
    private void requireMain() { if(!mainThread.getAsBoolean()) throw new IllegalStateException("SurfaceImpact requires server thread"); }
    public boolean isIdle() { return cancelled || (craterDone() && scarsPrepared && nextScar==scars.size() && burns.isEmpty()); }
    /** Counts direct block writes only; natural fire side effects are not counted. */
    public long changedBlocks() { return changed; }
    /** Retained for callers; this job no longer creates native explosions. */
    public long explosions() { return 0; }
    public long burnedEntities() { return burnedCount; }
    public void cancel() { requireMain(); cancelled=true; burns.clear(); scars.clear(); burned.clear(); }
}
