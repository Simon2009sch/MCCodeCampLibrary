package me.simoncrafter.mCCodeCampLibrary.input.activation;

import org.bukkit.block.Block;

public abstract class BlockActivationEvent extends ActivationEvent {

    private final Block block;

    public BlockActivationEvent(String id, Block block) {
        super(id);
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }
}
