package me.simoncrafter.mCCodeCampLibrary.animation;

import java.util.*;

/** Exact-cell ownership, including unloaded objects and pending recovery; holes do not claim blocks. */
final class AnimationSourceClaims {
    record Cell(int x,int y,int z) { }
    private record Owned(UUID world,Set<Cell> cells) { }
    private final Map<UUID,Owned> objects=new HashMap<>();
    void check(UUID owner,UUID world,Collection<Cell> cells) {
        Set<Cell> selected=new HashSet<>(cells);
        for(var entry:objects.entrySet()) {
            if(entry.getKey().equals(owner)||!entry.getValue().world().equals(world))continue;
            for(Cell cell:entry.getValue().cells())if(selected.contains(cell))
                throw new IllegalArgumentException("Source overlaps animation "+entry.getKey()+" at "+cell);
        }
    }
    void replace(UUID owner,UUID world,Collection<Cell> cells) {
        check(owner,world,cells);objects.put(owner,new Owned(world,Set.copyOf(cells)));
    }
    void release(UUID owner) {objects.remove(owner);}
}
