package me.simoncrafter.mCCodeCampLibrary.animation.schematic;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import java.util.LinkedHashMap;
import java.util.Map;

/** Publishes real restored states after WorldEdit has flushed; prevents server-only/ghost block restores. */
final class BlockRestoreUpdates {
    private BlockRestoreUpdates() { }
    static void publish(World world,Map<AnimatedSchematicStore.BlockPos,BlockData> restored) {
        if(restored.isEmpty())return;
        Map<Location,BlockData> updates=new LinkedHashMap<>();
        for(var entry:restored.entrySet()) {
            var p=entry.getKey();
            world.getBlockAt(p.x(),p.y(),p.z()).getState().update(true,false);
            updates.put(new Location(world,p.x(),p.y(),p.z()),entry.getValue());
        }
        for(var viewer:world.getPlayers())viewer.sendMultiBlockChange(updates);
    }
}
