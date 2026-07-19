package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public abstract class ABlockListEditable extends AInWorldEditable {

    protected List<Vector> blockList = new ArrayList<>();

    protected List<Vector> getBlockList() {
        return blockList;
    }
    protected void addBlock(Vector block) {
        if (!blockList.contains(block)) blockList.add(block);
    }
    protected void addBlock(Block block) {
        addBlock(getLocation().clone().subtract(block.getLocation()).toVector());
    };
    protected void addBlocks(Vector start, Vector end) {
        loopTroughCords(start, end, (vec) -> {if (!blockList.contains(vec)) blockList.add(vec);});
    }
    protected void removeBlock(Vector block) {
        blockList.remove(block);
    }
    protected void removeBlock(Block block) {
        removeBlock(getLocation().clone().subtract(block.getLocation()).toVector());
    };
    protected void removeBlocks(Vector start, Vector end) {
        loopTroughCords(start, end, (vec) -> {blockList.remove(vec);});
    }

    private void loopTroughCords(Vector start, Vector end, Consumer<Vector> action) {
        Vector diff = end.subtract(start);
        for (float x = 0; x > diff.getX(); x++) {
            for (float y = 0; y > diff.getY(); y++) {
                for (float z = 0; z > diff.getZ(); z++) {
                    Vector vec = new Vector(x, y, z);
                    action.accept(vec);
                }
            }
        }
    }
}
