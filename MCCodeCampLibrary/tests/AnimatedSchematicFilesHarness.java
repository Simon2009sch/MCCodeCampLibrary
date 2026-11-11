package me.simoncrafter.mCCodeCampLibrary.animation.schematic;
import java.nio.file.*;
import java.util.*;
public class AnimatedSchematicFilesHarness {
 public static void main(String[] args)throws Exception {
  Path dir=Files.createTempDirectory(Path.of(System.getenv("TMPDIR")),"animated-test-");
  try {
   var files=new SchematicFiles(dir); UUID id=UUID.randomUUID();
   byte[] schem={1,2,3}; byte[] metadata={4,5};
   files.commit(id,schem,metadata);
   assert Arrays.equals(files.read(id).schematic(),schem);
   assert Arrays.equals(files.read(id).metadata(),metadata);
   try {files.commit(id,new byte[]{9},metadata);throw new AssertionError("overwrite accepted");}catch(java.io.IOException expected){}
   assert Arrays.equals(files.read(id).schematic(),schem);
   Files.write(dir.resolve(id+".schem"),new byte[]{7});
   try {files.read(id);throw new AssertionError("corruption accepted");}catch(java.io.IOException expected){}
   try {files.read(UUID.randomUUID());throw new AssertionError("missing accepted");}catch(java.io.IOException expected){}
   UUID editable=UUID.randomUUID();
   files.commit(editable,schem,metadata);
   files.replace(editable,new byte[]{8,9},new byte[]{6});
   assert Arrays.equals(files.read(editable).schematic(),new byte[]{8,9});
   assert Arrays.equals(files.read(editable).metadata(),new byte[]{6});
   assert Arrays.equals(Files.readAllBytes(dir.resolve(editable+".schem")),schem); // Last good initial capture survives recapture.
   System.out.println("PASS durable commit, reload, overwrite refusal, corruption, missing snapshot, explicit recapture");
  } finally {try(var paths=Files.walk(dir)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
 }
}
