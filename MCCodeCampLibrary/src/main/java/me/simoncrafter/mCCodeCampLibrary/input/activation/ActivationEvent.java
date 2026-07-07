package me.simoncrafter.mCCodeCampLibrary.input.activation;

import org.bukkit.event.Event;

public abstract class ActivationEvent extends Event {

    private final String id;

    protected ActivationEvent(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
