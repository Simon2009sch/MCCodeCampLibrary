package me.simoncrafter.mCCodeCampLibrary.internal.events;

import org.bukkit.event.Event;

public abstract class IDEvent extends Event {

    private final String id;

    protected IDEvent(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
