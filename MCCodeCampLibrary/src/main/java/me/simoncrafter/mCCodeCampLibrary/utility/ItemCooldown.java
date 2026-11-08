package me.simoncrafter.mCCodeCampLibrary.utility;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.Objects;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Tick-dependent cooldown stored on the supplied item, not its player or handler.
 * Uses a dedicated PDC key without changing custom-item JSON or other metadata.
 * A cloned stack inherits its saved cooldown; otherwise stacks remain independent.
 * All methods must be called on the server thread. No item references are retained.
 */
public final class ItemCooldown {
    private static long currentTick;
    private static String currentSession = UUID.randomUUID().toString();
    private final NamespacedKey key;
    private final LongSupplier ticks;
    private final Supplier<String> session;

    /** Uses the central clock driven by {@link #tick()}; no tasks or listeners are created. */
    public ItemCooldown(NamespacedKey key) {
        this(key, () -> currentTick, () -> currentSession);
    }

    /** The clock must advance only with server ticks; use a new nonce on every restart. */
    public ItemCooldown(NamespacedKey key, LongSupplier ticks, String session) {
        this(key, ticks, () -> session);
        Objects.requireNonNull(session, "session");
    }

    private ItemCooldown(NamespacedKey key, LongSupplier ticks, Supplier<String> session) {
        this.key = Objects.requireNonNull(key, "key");
        this.ticks = Objects.requireNonNull(ticks, "ticks");
        this.session = session;
    }

    /** Call exactly once from the existing synchronous library per-tick loop. */
    public static void tick() {
        if (currentTick == Long.MAX_VALUE) resetSession();
        else currentTick++;
    }

    /**
     * Call on central loop start/stop, including re-enable. Existing helpers see the new nonce.
     * A fresh classloader also generates a new nonce after an unclean shutdown.
     */
    public static void resetSession() {
        currentTick = 0;
        currentSession = UUID.randomUUID().toString();
    }

    /**
     * Returns true and records an accepted use; false does not extend the cooldown.
     * Duration is ticks since the last accepted use; zero always allows, negatives throw.
     * Old sessions, malformed state and clock regressions allow a fresh use.
     */
    public boolean tryUse(ItemStack item, long durationTicks) {
        Objects.requireNonNull(item, "item");
        if (durationTicks < 0) throw new IllegalArgumentException("durationTicks must not be negative");
        long now = ticks.getAsLong();
        String prefix = session.get() + ":";
        String saved = item.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (saved != null && saved.startsWith(prefix)) {
            try {
                long started = Long.parseLong(saved.substring(prefix.length()));
                if (now >= started && now - started >= 0 && now - started < durationTicks) return false;
            } catch (NumberFormatException ignored) {
                // Invalid persisted state must not permanently lock the item.
            }
        }
        item.editPersistentDataContainer(data -> data.set(key, PersistentDataType.STRING, prefix + now));
        return true;
    }
}
