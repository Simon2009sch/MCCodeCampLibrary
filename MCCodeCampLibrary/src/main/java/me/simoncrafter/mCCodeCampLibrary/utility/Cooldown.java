package me.simoncrafter.mCCodeCampLibrary.utility;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class Cooldown {
    private final Plugin plugin;
    private int cooldown;
    private boolean finished = true;
    private BukkitTask cooldownTask;
    private Runnable callback = null;

    public Cooldown(Plugin plugin, int cooldown) {
        this.cooldown = cooldown;
        this.plugin = plugin;
    }

    public void registerCallback(Runnable callback) {
        this.callback = callback;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    /** True once the cooldown has elapsed (or was never started) — safe to use again. */
    public boolean isReady() {
        return finished;
    }

    /**
     * Starts the cooldown. If one is already running, {@code override} decides whether
     * it gets cancelled and restarted (true) or the call is ignored (false).
     */
    public void start(boolean override) {
        if (cooldown <= 0) {
            finish();
            return;
        }
        if (!finished && !override) {
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
    }

    public void forceEnd(boolean finish) {
        if (cooldownTask != null) {
            cooldownTask.cancel();
        }
        if (finish) {
            finish();
        }
    }

    private void finish() {
        finished = true;
        cooldownTask = null;
        if (callback != null) {
            callback.run();
        }
    }
}
