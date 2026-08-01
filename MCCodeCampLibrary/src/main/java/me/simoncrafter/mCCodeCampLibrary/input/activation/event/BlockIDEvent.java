package me.simoncrafter.mCCodeCampLibrary.input.activation.event;

import org.bukkit.block.Block;

public interface BlockIDEvent extends IDEvent {
    Block getBlock();
}
