package me.simoncrafter.mCCodeCampLibrary.input.activation.event.entityActivation;

import me.simoncrafter.mCCodeCampLibrary.input.activation.event.EntityIDEvent;
import org.bukkit.entity.Entity;

public interface EntityTargetIDEvent extends EntityIDEvent {
    Entity getTarget();
}
