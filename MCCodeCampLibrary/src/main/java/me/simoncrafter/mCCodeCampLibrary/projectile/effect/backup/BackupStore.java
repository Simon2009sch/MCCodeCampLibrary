package me.simoncrafter.mCCodeCampLibrary.projectile.effect.backup;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static java.nio.file.StandardOpenOption.*;

/** Durable journal; a numeric .meta file is the commit record for its matching schematic. */
final class BackupStore implements AutoCloseable {
 interface Backend {
  void capture(TerrainBackups.Backup backup) throws Exception;
  void restore(TerrainBackups.Backup backup) throws Exception;
 }
 private final Path directory;
 private final Backend backend;
 private final NavigableMap<Long,TerrainBackups.Backup> backups=new TreeMap<>();
 private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"terrain-backups");t.setDaemon(true);return t;});
 private long sequence;
 private int pending;
 private boolean closed;
 BackupStore(Path directory,Backend backend) throws IOException {
  this.directory=directory.toAbsolutePath();this.backend=backend;
  Files.createDirectories(this.directory);
  if(Files.exists(this.directory.resolve("sequence"))) {
   try {sequence=Long.parseLong(Files.readString(this.directory.resolve("sequence")).trim());if(sequence<0)throw new NumberFormatException();}
   catch(NumberFormatException e){throw new IOException("Invalid sequence journal",e);}
  }
  try(var files=Files.list(this.directory)) {
   for(Path file:files.toList()) {
    String name=file.getFileName().toString();
    if(name.matches("[0-9]+\\.(meta|schem|part)")) sequence=Math.max(sequence,Long.parseLong(name.substring(0,name.indexOf('.'))));
    if(!name.endsWith(".meta"))continue;
    try {
     Properties p=new Properties();try(var in=Files.newInputStream(file)){p.load(in);}
     long id=Long.parseLong(p.getProperty("sequence"));
     if(id<=0||!name.equals(id+".meta"))throw new IllegalArgumentException("Invalid sequence");
     var b=new TerrainBackups.Bounds(Integer.parseInt(p.getProperty("minX")),Integer.parseInt(p.getProperty("minY")),Integer.parseInt(p.getProperty("minZ")),Integer.parseInt(p.getProperty("maxX")),Integer.parseInt(p.getProperty("maxY")),Integer.parseInt(p.getProperty("maxZ")));
     Path schematic=this.directory.resolve(id+".schem");
     if(!Files.isRegularFile(schematic))throw new IOException("Missing schematic: "+id);
     String world=Objects.requireNonNull(p.getProperty("worldName"));
     backups.put(id,new TerrainBackups.Backup(id,UUID.fromString(p.getProperty("worldId")),world,b,schematic));
    }catch(Exception e){throw new IOException("Invalid backup metadata: "+file.getFileName(),e);}
   }
  }
 }
 synchronized int count(){return backups.size();}
 synchronized boolean busy(){return pending>0;}
 private synchronized void check(){if(closed||Thread.currentThread().isInterrupted())throw new CancellationException("Terrain backups closed");}
 synchronized CompletableFuture<TerrainBackups.Backup> capture(UUID world,String name,TerrainBackups.Bounds bounds) {
  return submit(()->{
   long id; synchronized(this){check();id=Math.incrementExact(sequence);sequence=id;
    Path next=directory.resolve("sequence.part");Files.writeString(next,Long.toString(id));force(next);
    Files.move(next,directory.resolve("sequence"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
   }
   // Reserve sequence before doing world work, including failed/incomplete captures.
   Path part=directory.resolve(id+".part");Files.createFile(part);
   var temporary=new TerrainBackups.Backup(id,world,name,bounds,part);
   backend.capture(temporary);check();force(part);
   var backup=new TerrainBackups.Backup(id,world,name,bounds,directory.resolve(id+".schem"));
   synchronized(this){check();Files.move(part,backup.schematicPath(),StandardCopyOption.ATOMIC_MOVE);
    Properties p=new Properties();p.setProperty("sequence",Long.toString(id));p.setProperty("worldId",world.toString());p.setProperty("worldName",name);
    p.setProperty("minX",""+bounds.minX());p.setProperty("minY",""+bounds.minY());p.setProperty("minZ",""+bounds.minZ());p.setProperty("maxX",""+bounds.maxX());p.setProperty("maxY",""+bounds.maxY());p.setProperty("maxZ",""+bounds.maxZ());
    Path meta=directory.resolve(id+".meta.part");try(var out=Files.newOutputStream(meta)){p.store(out,"Terrain backup v1");}force(meta);
    Files.move(meta,directory.resolve(id+".meta"),StandardCopyOption.ATOMIC_MOVE);backups.put(id,backup);
   }return backup;
  });
 }
 synchronized CompletableFuture<Integer> restoreAll(){return submit(()->{
  int count=0;
  while(true){TerrainBackups.Backup b;synchronized(this){check();if(backups.isEmpty())return count;b=backups.lastEntry().getValue();}
   backend.restore(b);
   synchronized(this){check(); // A failed or interrupted paste never reaches journal deletion.
    Files.delete(directory.resolve(b.sequence()+".meta"));backups.remove(b.sequence());
    Files.deleteIfExists(b.schematicPath());
   }count++;
  }
 });}
 private synchronized <T> CompletableFuture<T> submit(Callable<T> action){
  check();CompletableFuture<T> future=new CompletableFuture<>();pending++;
  executor.execute(()->{T value=null;Throwable failure=null;try{check();value=action.call();}catch(Throwable t){failure=t;}
   synchronized(this){if(pending>0)pending--;if(!closed){if(failure==null)future.complete(value);else future.completeExceptionally(failure);}}
  });return future;
 }
 private static void force(Path file)throws IOException{try(FileChannel c=FileChannel.open(file,WRITE)){c.force(true);}}
 /** Deliberately leaves unfinished futures unresolved: no completion callbacks after close. */
 public synchronized void close(){if(closed)return;closed=true;executor.shutdownNow();pending=0;}
}
