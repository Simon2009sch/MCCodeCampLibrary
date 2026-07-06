package me.simoncrafter.mCCodeCampLibrary.input.button;

import me.simoncrafter.mCCodeCampLibrary.internal.BlockMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.utility.Cooldown;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Owns every registered button: where it is, its cooldown, and the callback that
 * fires on press. Wires itself into {@link BlockMarkerRegistry} so a button's block
 * stays tagged (and therefore rediscoverable) across chunk load/unload.
 */
public class ButtonHandler {

    public static final String MARKER_TYPE = "button";

    private final Plugin plugin;
    private final BlockMarkerRegistry markerRegistry;
    private final Map<String, Button> buttons = new HashMap<>();
    private final Map<String, Consumer<Player>> callbacks = new HashMap<>();

    public ButtonHandler(Plugin plugin, BlockMarkerRegistry markerRegistry) {
        this.plugin = plugin;
        this.markerRegistry = markerRegistry;
        markerRegistry.registerLoadHandler(MARKER_TYPE, this::onMarkerLoad);
        markerRegistry.registerUnloadHandler(MARKER_TYPE, this::onMarkerUnload);
    }

    /** Convenience for the common case: no custom Button subclass needed. */
    public SimpleButton register(Location location, String id, int cooldownTicks, Consumer<Player> callback) {
        SimpleButton button = new SimpleButton(location, id, new Cooldown(plugin, cooldownTicks));
        register(button, callback);
        return button;
    }

    /** Registers a button (any subclass) and tags its block so it's found again after a reload. */
    public void register(Button button, Consumer<Player> callback) {
        buttons.put(button.getId(), button);
        callbacks.put(button.getId(), callback);
        markerRegistry.setMarker(button.getLocation().getBlock(), MARKER_TYPE, button.getId());
    }

    public void unregister(String id) {
        Button button = buttons.remove(id);
        callbacks.remove(id);
        if (button != null) {
            markerRegistry.removeMarker(button.getLocation().getBlock());
        }
    }

    /** Called by the interact listener once it's confirmed the clicked block is a registered button. */
    public void press(String id, Player player) {
        Button button = buttons.get(id);
        Consumer<Player> callback = callbacks.get(id);
        if (button == null || callback == null) return;

        Cooldown cooldown = button.getCooldown();
        if (!cooldown.isReady()) return;

        cooldown.start(false);
        callback.accept(player);
    }

    private void onMarkerLoad(String id) {
        // buttons/callbacks are populated by register() ahead of time, independent of
        // chunk state, so there's nothing to hydrate yet. Hook kept symmetric with
        // onMarkerUnload for whenever a button gains per-chunk-lifetime state.
    }

    private void onMarkerUnload(String id) {
        // Nothing to release today — see onMarkerLoad.
    }
}
