package me.simoncrafter.mCCodeCampLibrary.input.activation;

import me.simoncrafter.mCCodeCampLibrary.internal.events.IDEvent;
import org.bukkit.block.Block;

public abstract class BlockIDEvent extends IDEvent {

    private final Block block;

    public BlockIDEvent(String id, Block block) {
        super(id);
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }
}
