package me.simoncrafter.mCCodeCampLibrary.input.activation;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class PlayerBlockActivationEvent extends PlayerActivationEvent {

    private final Block block;

    public PlayerBlockActivationEvent(@NotNull Player player, String id, Block block) {
        super(player, id);
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }
}
