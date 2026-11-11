package me.simoncrafter.mCCodeCampLibrary.animation.schematic;
import java.util.*;
import org.bukkit.util.Vector;
import me.simoncrafter.mCCodeCampLibrary.obstical.AOpenableObject.OpenableRegion;
import com.sk89q.worldedit.math.BlockVector3;
public class AnimatedSelectionHarness {
 public static void main(String[] args) {
  var a=new OpenableRegion(new Vector(-1,2,3),new Vector(0,2,3));
  var b=new OpenableRegion(new Vector(2,2,3),new Vector(2,2,3));
  var selected=AnimatedSchematicStore.select(List.of(a,a,b));
  assert selected.size()==3; assert !selected.contains(BlockVector3.at(1,2,3));
  try{selected.clear();throw new AssertionError("mutable selection");}catch(UnsupportedOperationException expected){}
  try{AnimatedSchematicStore.select(List.of());throw new AssertionError("empty accepted");}catch(IllegalArgumentException expected){}
  try{AnimatedSchematicStore.select(List.of(new OpenableRegion(new Vector(Double.NaN,0,0),new Vector())));throw new AssertionError("NaN accepted");}catch(IllegalArgumentException expected){}
  System.out.println("PASS multi-area union, overlap dedupe, holes, immutable selection, invalid bounds");
 }
}
