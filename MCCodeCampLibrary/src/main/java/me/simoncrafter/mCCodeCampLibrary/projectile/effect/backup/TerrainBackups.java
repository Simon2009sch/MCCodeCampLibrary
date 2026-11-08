package me.simoncrafter.mCCodeCampLibrary.projectile.effect.backup;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Serial, durable terrain snapshots. Construct/capture/restore on the Bukkit main thread.
 * Completion runs on the owned worker; consumers must marshal Bukkit callbacks to the main thread.
 * FAWE is mandatory: vanilla WorldEdit is NOT safe for this background use.
 */
public final class TerrainBackups implements AutoCloseable {
 public record Bounds(int minX,int minY,int minZ,int maxX,int maxY,int maxZ) {
  public Bounds {
   if(minX>maxX||minY>maxY||minZ>maxZ)throw new IllegalArgumentException("Inverted bounds");
   long x=(long)maxX-minX+1,y=(long)maxY-minY+1,z=(long)maxZ-minZ+1;
   if(x>20_000_000||y>20_000_000||z>20_000_000||x*y>20_000_000||x*y*z>20_000_000)throw new IllegalArgumentException("Region exceeds 20 million blocks");
  }
 }
 public record Backup(long sequence,UUID worldId,String worldName,Bounds bounds,Path schematicPath) {}
 private final Plugin plugin;
 private final BackupStore store;
 private volatile boolean closed;
 public TerrainBackups(Plugin plugin,Path directory) {
  this.plugin=Objects.requireNonNull(plugin);requireMain();requireFawe();
  try {store=new BackupStore(directory,new BackupStore.Backend(){
   public void capture(Backup b)throws Exception{copy(b);}
   public void restore(Backup b)throws Exception{paste(b);}
  });}catch(IOException e){throw new UncheckedIOException("Cannot open terrain backup journal",e);}
 }
 public CompletableFuture<Backup> capture(World world,Bounds bounds){
  requireMain();requireOpen();requireFawe();Objects.requireNonNull(world);Objects.requireNonNull(bounds);
  if(bounds.minY()<world.getMinHeight()||bounds.maxY()>=world.getMaxHeight())throw new IllegalArgumentException("Bounds outside world height");
  return store.capture(world.getUID(),world.getName(),bounds);
 }
 public CompletableFuture<Integer> restoreAll(){requireMain();requireOpen();requireFawe();return store.restoreAll();}
 public int backupCount(){return store.count();}
 public boolean isBusy(){return store.busy();}
 private void requireOpen(){if(closed)throw new IllegalStateException("TerrainBackups closed");}
 private static void requireMain(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Main thread required");}
 private void requireFawe(){
  if(!plugin.isEnabled()||!Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit"))throw new IllegalStateException("Enabled FastAsyncWorldEdit required");
 }
 private com.sk89q.worldedit.world.World world(Backup b)throws Exception {
  requireOpen();
  return Bukkit.getScheduler().callSyncMethod(plugin,()->{
   requireOpen();requireFawe();World w=Bukkit.getWorld(b.worldId());
   if(w==null)throw new IllegalStateException("Backup world unavailable: "+b.worldId()+" ("+b.worldName()+")");
   if(b.bounds().minY()<w.getMinHeight()||b.bounds().maxY()>=w.getMaxHeight())throw new IllegalStateException("Backup outside current world height");
   return BukkitAdapter.adapt(w);
  }).get();
 }
 private static BlockVector3 min(Bounds b){return BlockVector3.at(b.minX(),b.minY(),b.minZ());}
 private static BlockVector3 max(Bounds b){return BlockVector3.at(b.maxX(),b.maxY(),b.maxZ());}
 private void copy(Backup b)throws Exception {
  var world=world(b);var region=new CuboidRegion(world,min(b.bounds()),max(b.bounds()));
  var clipboard=new BlockArrayClipboard(region);clipboard.setOrigin(min(b.bounds()));
  try(var owned=ownClipboard(clipboard)) {
  try(var session=WorldEdit.getInstance().newEditSession(world)){
   var copy=new ForwardExtentCopy(session,region,clipboard,min(b.bounds()));
   copy.setCopyingEntities(false);copy.setCopyingBiomes(false);Operations.complete(copy);
  }
  requireOpen();((Flushable)(Object)clipboard).flush();
  try(var out=Files.newOutputStream(b.schematicPath());var writer=BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(out)){writer.write(clipboard);}
  }
 }
 private static AutoCloseable ownClipboard(Object clipboard) {
  // FAWE adds these interfaces to the WorldEdit API. Do not silently fall back to vanilla WE.
  if(!(clipboard instanceof Flushable)||!(clipboard instanceof AutoCloseable owner))throw new IllegalStateException("FAWE clipboard implementation required");
  return owner;
 }
 private void paste(Backup b)throws Exception {
  var world=world(b);
  try(var in=Files.newInputStream(b.schematicPath());var reader=BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(in)){
   var clipboard=reader.read();
   try(var owned=ownClipboard(clipboard)) {
   ((Flushable)(Object)clipboard).flush();
   if(!clipboard.getDimensions().equals(max(b.bounds()).subtract(min(b.bounds())).add(1,1,1)))throw new IOException("Schematic dimensions differ from original bounds");
   clipboard.setOrigin(clipboard.getRegion().getMinimumPoint());requireOpen();
   try(var session=WorldEdit.getInstance().newEditSession(world)){
    Operations.complete(new ClipboardHolder(clipboard).createPaste(session).to(min(b.bounds())).ignoreAirBlocks(false).copyEntities(false).copyBiomes(false).build());
   } // FAWE close is blocking and flushes; journal deletion happens only after return.
   }
  }
 }
 /** Stop scheduling/interrupt owned work; retain unfinished files and do not complete pending futures. */
 @Override public void close(){closed=true;store.close();}
}
