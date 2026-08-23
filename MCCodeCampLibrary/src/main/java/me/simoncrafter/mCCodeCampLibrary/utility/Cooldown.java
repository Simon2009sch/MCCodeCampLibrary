package me.simoncrafter.mCCodeCampLibrary.utility;

import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.Set;

/**
 * A tick-based cooldown that does not schedule any Bukkit tasks.
 * <p>
 * Elapsed detection is lazy: {@link #isOnCooldown()} compares the current tick
 * against the stored release tick, and {@link #start(boolean)} runs the elapse
 * callbacks if a previous cooldown has finished since the last check. No
 * scheduler round-trips, no task to cancel on destroy.
 */
public class Cooldown {

    private int cooldown;
    private long readyAtTick = 0;
    private long callbacksFiredForReadyAt = 0;
    private final Set<Runnable> callbacks = new HashSet<>();
    private final Set<Runnable> startCallbacks = new HashSet<>();

    public Cooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public void registerCallback(Runnable callback) {
        this.callbacks.add(callback);
    }

    public void registerStartCallback(Runnable callback) {
        this.startCallbacks.add(callback);
    }

    public int getCooldown() {
        return cooldown;
    }

    /**
     * Sets the cooldown time. Will not influence the currently running cooldown.
     * @param cooldown
     */
    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    /**
     * @return {@code true} if the cooldown hasn't elapsed yet. If the cooldown is over
     *         (or set to 0) returns {@code false}.
     */
    public boolean isOnCooldown() {
        return Bukkit.getCurrentTick() < readyAtTick;
    }

    /**
     * Starts the cooldown.
     */
    public void start() {
        start(false);
    }

    /**
     * Starts the cooldown. If one is already running, {@code override} decides whether
     * it gets restarted (true) or the call is ignored (false).
     */
    public void start(boolean overrideCurrentCooldown) {
        if (cooldown <= 0) {
            return;
        }
        if (isOnCooldown() && !overrideCurrentCooldown) {
            return;
        }
        fireElapsedCallbacks();
        readyAtTick = Bukkit.getCurrentTick() + cooldown;
        startCallbacks.forEach(Runnable::run);
    }

    /**
     * Fires the elapse callbacks if a cooldown has finished since they were last fired.
     * Safe to call at any time; each elapse fires the callbacks at most once.
     */
    private void fireElapsedCallbacks() {
        long currentTick = Bukkit.getCurrentTick();
        if (readyAtTick > 0 && readyAtTick <= currentTick && callbacksFiredForReadyAt != readyAtTick) {
            callbacksFiredForReadyAt = readyAtTick;
            callbacks.forEach(Runnable::run);
        }
    }
}
