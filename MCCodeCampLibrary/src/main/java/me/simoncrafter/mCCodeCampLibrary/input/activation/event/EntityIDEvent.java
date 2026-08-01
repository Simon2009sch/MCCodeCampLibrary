package me.simoncrafter.mCCodeCampLibrary.input.activation.event;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public interface EntityIDEvent extends IDEvent {
    @NotNull Entity getEntity();
}
