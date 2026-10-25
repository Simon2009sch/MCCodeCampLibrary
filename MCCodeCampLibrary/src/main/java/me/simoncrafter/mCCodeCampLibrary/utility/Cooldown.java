package me.simoncrafter.mCCodeCampLibrary.utility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A tick-based cooldown driven by the single loop in {@link MCCodeCampLib}.
 * Instances register themselves; owners must call {@link #dispose()} when removed.
 * Construction, mutation and {@link #tickAll()} must run on the server thread.
 * This class does not access Bukkit or create tasks itself.
 */
public class Cooldown {
    private static final List<Cooldown> instances = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(Cooldown.class.getName());
    private static long currentTick;

    private int cooldown;
    private long readyAtTick;
    private boolean running;
    private boolean disposed;
    private final Set<Runnable> callbacks = new HashSet<>();
    private final Set<Runnable> startCallbacks = new HashSet<>();

    public Cooldown(int cooldown) {
        this.cooldown = cooldown;
        instances.add(this);
    }

    /** Called once per server tick by the central library loop. */
    public static void tickAll() {
        currentTick++;
        // A callback may create or dispose cooldowns: never iterate the live list.
        for (Cooldown instance : List.copyOf(instances)) {
            instance.fireElapsedCallbacks();
        }
    }

    /** Stops and releases all registered instances when the library is disabled. */
    public static void disposeAll() {
        for (Cooldown instance : List.copyOf(instances)) {
            instance.dispose();
        }
        currentTick = 0;
    }

    /** Cancels without firing callbacks and permanently unregisters this instance. Idempotent. */
    public void dispose() {
        if (disposed) return;
        disposed = true;
        running = false;
        instances.remove(this);
        callbacks.clear();
        startCallbacks.clear();
    }

    public void registerCallback(Runnable callback) {
        if (!disposed) callbacks.add(Objects.requireNonNull(callback, "callback"));
    }

    public void registerStartCallback(Runnable callback) {
        if (!disposed) startCallbacks.add(Objects.requireNonNull(callback, "callback"));
    }

    public int getCooldown() {
        return cooldown;
    }

    /** Sets the duration for future starts; does not change an already running cooldown. */
    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public boolean isOnCooldown() {
        return !disposed && running && currentTick < readyAtTick;
    }

    public void start() {
        start(false);
    }

    /** Starts the cooldown; a running cooldown is restarted only when override is true. */
    public void start(boolean overrideCurrentCooldown) {
        if (disposed || cooldown <= 0) return;
        if (isOnCooldown() && !overrideCurrentCooldown) return;
        readyAtTick = currentTick + cooldown;
        running = true;
        runCallbacks(startCallbacks);
    }

    private void fireElapsedCallbacks() {
        if (disposed || !running || currentTick < readyAtTick) return;
        // Clear first: a callback may restart or dispose this cooldown.
        running = false;
        runCallbacks(callbacks);
    }

    private void runCallbacks(Set<Runnable> actions) {
        for (Runnable action : List.copyOf(actions)) {
            if (disposed) break;
            try {
                action.run();
            } catch (RuntimeException e) {
                // One broken callback must not prevent other cooldowns from expiring.
                LOGGER.log(Level.WARNING, "Cooldown callback failed", e);
            }
        }
    }
}
