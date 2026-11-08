package me.simoncrafter.mCCodeCampLibrary.projectile;

import me.simoncrafter.mCCodeCampLibrary.projectile.effect.Capsule;
import org.bukkit.util.Vector;

/** Pure geometry; no server bootstrap. */
public class CapsuleHarness {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        Vector start = new Vector(-10, -2, -3), end = new Vector(10, -2, -3);
        Capsule capsule = new Capsule(start, end, 4);
        check(capsule.contains(0, 2, -3), "cylinder boundary included");
        check(!capsule.contains(0, 2.001, -3), "outside cylinder excluded");
        check(capsule.contains(-14, -2, -3) && capsule.contains(14, -2, -3), "rounded endpoints included");
        check(!capsule.contains(-14, 2, -3), "bounding-box corner is not capsule");
        check(capsule.minX()==-14 && capsule.maxX()==14 && capsule.minY()==-6 && capsule.maxY()==2 && capsule.minZ()==-7 && capsule.maxZ()==1, "inclusive conservative voxel bounds");
        start.setX(500); end.setX(600);
        check(capsule.contains(0,-2,-3), "constructor snapshots input vectors");
        Capsule sphere = new Capsule(new Vector(-.2,-.2,-.2),new Vector(-.2,-.2,-.2),4);
        check(sphere.contains(3.8,-.2,-.2) && !sphere.contains(3.81,-.2,-.2), "zero-length sphere");
        check(sphere.minX()==-5 && sphere.maxX()==3, "negative fractional voxel bounds floor");
        Capsule diagonal = new Capsule(new Vector(),new Vector(10,10,10),4);
        check(diagonal.contains(5,5,5) && !diagonal.contains(5,10,0), "diagonal projection");
        for (double radius : new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY}) {
            try {new Capsule(new Vector(),new Vector(),radius);throw new AssertionError("invalid radius accepted");} catch(IllegalArgumentException expected) {}
        }
        check(new Capsule(new Vector(),new Vector(),0).contains(0,0,0), "zero radius allowed");
        for(int x=-16;x<=16;x++) for(int y=-8;y<=8;y++) for(int z=-9;z<=4;z++) {
            if(capsule.contains(x+.5,y+.5,z+.5)) check(x>=capsule.minX() && x<=capsule.maxX() && y>=capsule.minY() && y<=capsule.maxY() && z>=capsule.minZ() && z<=capsule.maxZ(),"bounds cover contained block centers");
        }
        System.out.println("Capsule geometry checks passed");
    }
}
