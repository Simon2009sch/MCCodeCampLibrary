package me.simoncrafter.mCCodeCampLibrary.input.activation;

import org.antlr.v4.tool.ast.ActionAST;
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
