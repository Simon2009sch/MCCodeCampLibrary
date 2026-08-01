package me.simoncrafter.mCCodeCampLibrary.input.activation.event;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface PlayerIDEvent extends IDEvent {
    @NotNull Player getPlayer();
}
