package me.simoncrafter.mCCodeCampLibrary.projectile.effect.backup;
import java.nio.file.*;
import java.util.*;
public final class TerrainBackupsHarness {
 public static void main(String[] args) throws Exception {
  Path p=Files.createTempDirectory(Path.of(System.getenv("TMPDIR")),"terrain-test-");
  try {
   var bounds=new TerrainBackups.Bounds(-1,-64,-1,1,319,1);
   List<Long> pasted=new ArrayList<>();
   var backend=new BackupStore.Backend(){
    public void capture(TerrainBackups.Backup b)throws Exception{Files.writeString(b.schematicPath(),"schematic");}
    public void restore(TerrainBackups.Backup b)throws Exception{pasted.add(b.sequence());}
   };
   try(var store=new BackupStore(p,backend)) {
    store.capture(UUID.randomUUID(),"world",bounds).get();
    store.capture(UUID.randomUUID(),"other",bounds).get();
    check(store.count()==2,"durable count");
   }
   try(var store=new BackupStore(p,backend)) {
    check(store.restoreAll().get()==2,"restored count");
    check(pasted.equals(List.of(2L,1L)),"LIFO restart");
    check(store.count()==0,"delete after success");
   }
   try(var store=new BackupStore(p,backend)) {
    check(store.capture(UUID.randomUUID(),"world",bounds).get().sequence()==3,"sequence survives drained journal");
   }
   List<Long> attempts=new ArrayList<>();
   var failing=new BackupStore.Backend(){
    public void capture(TerrainBackups.Backup b)throws Exception{backend.capture(b);}
    public void restore(TerrainBackups.Backup b)throws Exception{attempts.add(b.sequence());throw new java.io.IOException("world missing / paste failed");}
   };
   try(var store=new BackupStore(p,failing)) {
    store.capture(UUID.randomUUID(),"newest",bounds).get();
    try{store.restoreAll().get();throw new AssertionError("failure expected");}catch(java.util.concurrent.ExecutionException expected){}
    check(store.count()==2,"failure retains all backups");
    check(attempts.equals(List.of(4L)),"failure stops before older backup");
    check(Files.exists(p.resolve("4.schem")),"failed paste schematic retained");
   }
   try(var store=new BackupStore(p,backend)){check(store.restoreAll().get()==2,"retry succeeds");}
   Files.writeString(p.resolve("999.meta"),"bad metadata");
   try{new BackupStore(p,backend);throw new AssertionError("malformed must fail closed");}catch(java.io.IOException expected){}
   Files.delete(p.resolve("999.meta"));
   var brokenCapture=new BackupStore.Backend(){
    public void capture(TerrainBackups.Backup b)throws Exception{Files.writeString(b.schematicPath(),"partial");throw new java.io.IOException("capture failed");}
    public void restore(TerrainBackups.Backup b){throw new AssertionError("incomplete capture must not restore");}
   };
   try(var failedStore=new BackupStore(p,brokenCapture)) {
    try{failedStore.capture(UUID.randomUUID(),"world",bounds).get();throw new AssertionError("capture fails");}catch(java.util.concurrent.ExecutionException expected){}
    check(failedStore.count()==0,"failed capture not committed");
   }
   try(var reopened=new BackupStore(p,backend)){check(reopened.count()==0,"partial files ignored after restart");}
   var entered=new java.util.concurrent.CountDownLatch(1);
   var released=new java.util.concurrent.CountDownLatch(1);
   var finished=new java.util.concurrent.CountDownLatch(1);
   var blocking=new BackupStore.Backend(){
    public void capture(TerrainBackups.Backup b)throws Exception{entered.countDown();try{released.await();}finally{finished.countDown();}}
    public void restore(TerrainBackups.Backup b){}
   };
   var store=new BackupStore(p,blocking);
   var future=store.capture(UUID.randomUUID(),"blocked",bounds);
   check(entered.await(5,java.util.concurrent.TimeUnit.SECONDS),"worker entered");
   check(store.busy(),"busy while capturing");
   var queued=store.restoreAll();
   store.close();released.countDown();check(finished.await(5,java.util.concurrent.TimeUnit.SECONDS),"worker interrupted");
   Thread.sleep(50);
   check(!future.isDone()&&!queued.isDone(),"no completion callbacks after close");
   check(!store.busy(),"closed not busy");
   check(store.count()==0,"closed capture not committed");
   try{store.restoreAll();throw new AssertionError("closed reject");}catch(java.util.concurrent.CancellationException expected){}
   try{new TerrainBackups.Bounds(1,0,0,0,0,0);throw new AssertionError("inverted");}catch(IllegalArgumentException expected){}
   try{new TerrainBackups.Bounds(Integer.MIN_VALUE,0,0,Integer.MAX_VALUE,0,0);throw new AssertionError("huge");}catch(IllegalArgumentException expected){}
   System.out.println("PASS durable ordering, persistent sequence, failure retention/stop, successful deletion, malformed metadata, bounds, busy and close");
  } finally {try(var s=Files.walk(p)){for(Path f:s.sorted(Comparator.reverseOrder()).toList())Files.delete(f);}}
 }
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
}
