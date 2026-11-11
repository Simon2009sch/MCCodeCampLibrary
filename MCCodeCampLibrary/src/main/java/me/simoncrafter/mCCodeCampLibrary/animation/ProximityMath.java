package me.simoncrafter.mCCodeCampLibrary.animation;

import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Deterministic, server-independent storage placement and distance-driven paths. */
public final class ProximityMath {
    private ProximityMath() { }

    public static float progress(double distance, double near, double far) {
        return (float) Math.clamp((far - distance) / (far - near), 0, 1);
    }

    /** Quadratic Bezier, with exact storage/original endpoints. */
    public static Vector3f curve(Vector3f storage, Vector3f original, float t, float height) {
        Vector3f control = new Vector3f(storage).lerp(original, .5f).add(0, height, 0);
        return new Vector3f(storage).mul((1-t)*(1-t)).fma(2*(1-t)*t, control).fma(t*t, original);
    }

    public static Vector3f storageAngles(int x, int y, int z) {
        Random random = new Random(seed(x, y, z));
        return new Vector3f(random.nextFloat()*200-100, random.nextFloat()*200-100, random.nextFloat()*200-100);
    }

    public static long seed(int x, int y, int z) {
        long h = x * 0x632BE59BD9B4E019L ^ y * 0x9E3779B97F4A7C15L ^ z * 0x85157AF5D66D3E6BL;
        h ^= h >>> 33;
        return h;
    }

    /** Shuffled, lightly jittered lattice: at least 1.55 blocks between centers; fails if too small. */
    public static List<Vector3f> scatter(int count, Vector3f min, Vector3f maxExclusive, long seed) {
        List<Vector3f> slots = new ArrayList<>();
        Random random = new Random(seed);
        for (float x=min.x+.6f; x<=maxExclusive.x-.6f; x+=1.75f)
            for (float y=min.y+.6f; y<=maxExclusive.y-.6f; y+=1.75f)
                for (float z=min.z+.6f; z<=maxExclusive.z-.6f; z+=1.75f) {
                    if (slots.size() >= 100000) throw new IllegalArgumentException("Storage area is too large");
                    slots.add(new Vector3f(x+random.nextFloat()*.1f, y+random.nextFloat()*.1f, z+random.nextFloat()*.1f));
                }
        if (slots.size() < count) throw new IllegalArgumentException("Storage too small: needs " + count + " slots; has " + slots.size());
        Collections.shuffle(slots, random);
        return List.copyOf(slots.subList(0, count));
    }
}
