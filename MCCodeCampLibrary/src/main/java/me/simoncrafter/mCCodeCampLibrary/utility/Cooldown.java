package me.simoncrafter.mCCodeCampLibrary.utility;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

public class Cooldown {
    private final Plugin plugin;
    private int cooldown;
    private boolean finished = true;
    private BukkitTask cooldownTask;
    private Set<Runnable> callbacks = new HashSet<>();
    private Set<Runnable> startCallbacks = new HashSet<>();

    public Cooldown(Plugin plugin, int cooldown) {
        this.cooldown = cooldown;
        this.plugin = plugin;
    }

    public void registerCallback(Runnable callback) {
        this.callbacks.add(callback);
    }

    public void registerStartCallback(Runnable callback) {
        this.callbacks.add(callback);
    }

    public int getCooldown() {
        return cooldown;
    }

    /**
     * Sets the cooldown time. Will not influence the currently running cooldown
     * @param cooldown
     */
    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    /**
     * @return Returns true if the cooldown hasn't elapsed jet. If the cooldown is over returns false
     */
    public boolean isOnCooldown() {
        return finished;
    }

    /**
     * Starts the cooldown
     */
    public void start() {
        start(false);
    }

    /**
     * Starts the cooldown. If one is already running, {@code override} decides whether
     * it gets cancelled and restarted (true) or the call is ignored (false).
     */
    public void start(boolean overrideCurrentCooldown) {
        if (cooldown <= 0) {
            finish();
            return;
        }
        if (!finished && !overrideCurrentCooldown) {
            return;
        }
        if (cooldownTask != null) {
            cooldownTask.cancel();
        }
        finished = false;
        cooldownTask = new BukkitRunnable() {
            @Override
            public void run() {
                finish();
            }
        }.runTaskLater(plugin, cooldown);
        startCallbacks.forEach(Runnable::run);
    }

    public void forceEnd(boolean callFinishEvent) {
        if (cooldownTask != null) {
            cooldownTask.cancel();
        }
        if (callFinishEvent) {
            finish();
        }
    }

    private void finish() {
        finished = true;
        cooldownTask = null;
        if (callbacks != null) {
            callbacks.forEach(Runnable::run);
        }
    }
}
