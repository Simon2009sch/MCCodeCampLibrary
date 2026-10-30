package me.simoncrafter.mCCodeCampLibrary.internal.editor.display;

import me.simoncrafter.CraftersDisplayLibrary.core.PositionObject;
import me.simoncrafter.CraftersDisplayLibrary.display.cube.CubeColorDisplay;
import me.simoncrafter.CraftersDisplayLibrary.display.cube.CubeColorInformation;
import me.simoncrafter.CraftersDisplayLibrary.display.panel.TextDisplay;
import me.simoncrafter.CraftersDisplayLibrary.display.wireframecube.WireframeCubeColorDisplay;
import me.simoncrafter.CraftersDisplayLibrary.display.wireframecube.WireframeCubeColorInformation;
import me.simoncrafter.CraftersDisplayLibrary.entity.InteractionBox;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A positionable parent for an object's editor visuals and click target.
 * Moving, rotating or scaling this object uses the library's inherited behavior.
 * All four layers are siblings, so hiding the cube does not hide its label or hitbox.
 *
 * <p>Defaults are centered at local (0.5, 0.5, 0.5), matching a block marker whose
 * location is the block origin. Custom factories can use different local offsets/shapes.
 * Use on the server thread and call {@link #remove()} on owner removal/unload.
 */
public class EditableDisplay extends PositionObject {

    /** Colors and selection line thickness passed to each creation function. */
    public record Style(Color displayColor, Color selectionColor, float selectionThickness) {}

    private final Plugin plugin;
    private PositionObject display;
    private PositionObject label;
    private PositionObject selectionDisplay;
    private PositionObject interactionBox;
    private final Set<Player> visibleToPlayers = new HashSet<>();
    private final Set<Player> selectedByPlayers = new HashSet<>();

    // Preserve the appearance chosen in the author's scaffold.
    private Style style = new Style(Color.fromARGB(240, 0, 0, 0), Color.YELLOW, 0.01f);
    private Component labelText = Component.empty();
    private Consumer<PlayerInteractAtEntityEvent> onRightClick = event -> {};

    private Function<Style, PositionObject> displayCreationMethod = this::createDefaultDisplay;
    private Function<Style, PositionObject> labelCreationMethod = this::createDefaultLabel;
    private Function<Style, PositionObject> selectionCreationMethod = this::createDefaultSelection;
    private BiFunction<Style, Consumer<PlayerInteractAtEntityEvent>, PositionObject> interactionCreationMethod = this::createDefaultInteraction;

    /** Creates an unspawned helper at the supplied block origin. */
    public EditableDisplay(Location location, Plugin plugin) {
        super(List.of(), new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1), new Quaternionf()),
                Objects.requireNonNull(location, "location").clone());
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /** Creates a labeled, clickable marker. Nothing spawns until shown. */
    public EditableDisplay(Location location, Plugin plugin, Component labelText,
                           Consumer<PlayerInteractAtEntityEvent> onRightClick) {
        this(location, plugin);
        setLabelText(labelText);
        setOnRightClick(onRightClick);
    }

    public Style getStyle() {
        return style;
    }

    /** Changes the style for the next creation; call rebuild() to update existing layers. */
    public void setStyle(Style style) {
        this.style = Objects.requireNonNull(style, "style");
    }

    /** Updates the default label immediately if it is already spawned. */
    public void setLabelText(Component labelText) {
        this.labelText = Objects.requireNonNull(labelText, "labelText");
        if (label instanceof TextDisplay textDisplay) textDisplay.setText(labelText);
    }

    /** Forwards the callback to the default InteractionBox without wrapping the event. */
    public void setOnRightClick(Consumer<PlayerInteractAtEntityEvent> callback) {
        onRightClick = Objects.requireNonNull(callback, "callback");
        if (interactionBox instanceof InteractionBox box) {
            box.setOnRightClick(callback);
        } else if (interactionBox != null) {
            // Custom backends receive callbacks through their creation function.
            rebuild();
        }
    }

    /**
     * Replaces the normal display constructor. Factories return fresh, spawned library objects
     * at getLocation(), with offsets in their local transform. The helper parents and owns them.
     * Configure before showing, or call rebuild() afterward; the other layers are independent.
     */
    public void setDisplayCreationMethod(Function<Style, PositionObject> method) {
        displayCreationMethod = Objects.requireNonNull(method, "method");
    }

    /** Replaces the label constructor under the same ownership rules as the normal display. */
    public void setLabelCreationMethod(Function<Style, PositionObject> method) {
        labelCreationMethod = Objects.requireNonNull(method, "method");
    }

    /** Replaces the selection constructor, called only when a visible viewer selects. */
    public void setSelectionCreationMethod(Function<Style, PositionObject> method) {
        selectionCreationMethod = Objects.requireNonNull(method, "method");
    }

    /** Replaces the interaction constructor, passing the original right-click callback through. */
    public void setInteractionCreationMethod(
            BiFunction<Style, Consumer<PlayerInteractAtEntityEvent>, PositionObject> method) {
        interactionCreationMethod = Objects.requireNonNull(method, "method");
    }

    /** Adds a layer to this parent's actual library transform hierarchy. */
    private PositionObject attach(PositionObject child) {
        Objects.requireNonNull(child, "Creation functions must return a display");
        child.hideByDefault(true, true);
        addChild(child);
        return child;
    }

    /** Allocates ordinary layers once; selection remains lazy. */
    private void createDisplays() {
        if (display != null) return;
        try {
            display = attach(displayCreationMethod.apply(style));
            label = attach(labelCreationMethod.apply(style));
            interactionBox = attach(interactionCreationMethod.apply(style, onRightClick));
        } catch (RuntimeException | Error failure) {
            clearDisplays();
            throw failure;
        }
    }

    /** Shows the label/hitbox and the appropriate cube for this player. Duplicate calls do nothing. */
    public void showFor(Player player) {
        Objects.requireNonNull(player, "player");
        if (visibleToPlayers.contains(player)) return;
        createDisplays();
        visibleToPlayers.add(player);
        label.showForPlayer(player, true);
        interactionBox.showForPlayer(player, true);
        display.showForPlayer(player, true);
        updateSelection(player);
    }

    /** Hides every layer and clears this viewer's selection; the last viewer releases entities. */
    public void hideFor(Player player) {
        selectedByPlayers.remove(player);
        if (!visibleToPlayers.remove(player)) return;
        display.hideForPlayer(player, true);
        label.hideForPlayer(player, true);
        interactionBox.hideForPlayer(player, true);
        if (selectionDisplay != null) selectionDisplay.hideForPlayer(player, true);
        if (visibleToPlayers.isEmpty()) clearDisplays();
    }

    /** Remembers hidden selections without spawning; visible selections add the wireframe overlay. */
    public void select(Player player) {
        Objects.requireNonNull(player, "player");
        if (selectedByPlayers.add(player) && visibleToPlayers.contains(player)) updateSelection(player);
    }

    /** Hides the wireframe overlay, leaving the cube, label and hitbox available. */
    public void deselect(Player player) {
        if (selectedByPlayers.remove(player) && visibleToPlayers.contains(player)) updateSelection(player);
    }

    private void updateSelection(Player player) {
        if (selectedByPlayers.contains(player)) {
            if (selectionDisplay == null) selectionDisplay = attach(selectionCreationMethod.apply(style));
            selectionDisplay.showForPlayer(player, true);
        } else {
            if (selectionDisplay != null) selectionDisplay.hideForPlayer(player, true);
        }
    }

    /** Re-evaluates the constructors and restores the current viewers and selections. */
    public void rebuild() {
        clearDisplays();
        if (visibleToPlayers.isEmpty()) return;
        createDisplays();
        for (Player player : visibleToPlayers) {
            label.showForPlayer(player, true);
            interactionBox.showForPlayer(player, true);
            display.showForPlayer(player, true);
            updateSelection(player);
        }
    }

    /** Detaches removed layers so inherited movement never updates stale children. */
    private void clearDisplays() {
        removeLayer(display);
        removeLayer(label);
        removeLayer(selectionDisplay);
        removeLayer(interactionBox);
        display = label = selectionDisplay = interactionBox = null;
    }

    private void removeLayer(PositionObject layer) {
        if (layer != null) {
            removeChild(layer);
            layer.remove();
        }
    }

    /** Removes owned visuals/listeners and clears viewer state. Safe to call repeatedly. */
    @Override
    public void remove() {
        clearDisplays();
        visibleToPlayers.clear();
        selectedByPlayers.clear();
        super.remove();
    }

    // Delegate all rendering and click detection to CraftersDisplayLibrary.
    private PositionObject createDefaultDisplay(Style style) {
        CubeColorDisplay cube = CubeColorDisplay.create(getLocation(), new Vector3f(1.01f),
                new Vector3f(), new Quaternionf(), new CubeColorInformation(style.displayColor()), true);
        cube.hideByDefault(true);
        cube.spawnDisplay();
        return cube;
    }

    private PositionObject createDefaultLabel(Style style) {
        TextDisplay text = TextDisplay.create(getLocation(), new Vector3f(1), new Vector3f(), new Quaternionf());
        text.setBillboard(Display.Billboard.CENTER);
        text.setSeeThrough(true);
        text.setText(labelText);
        text.hideByDefault(true);
        text.spawnDisplay();
        return text;
    }

    private PositionObject createDefaultSelection(Style style) {
        WireframeCubeColorDisplay wireframe = WireframeCubeColorDisplay.create(getLocation(), new Vector3f(1.05f),
                new Vector3f(), new Quaternionf(), new WireframeCubeColorInformation(style.selectionColor()),
                true, style.selectionThickness());
        wireframe.hideByDefault(true);
        wireframe.spawnDisplay();
        return wireframe;
    }

    private PositionObject createDefaultInteraction(Style style, Consumer<PlayerInteractAtEntityEvent> callback) {
        InteractionBox box = InteractionBox.create(getLocation(), new Vector3f(1.05f), new Vector3f(0, -0.01f, 0), plugin); // small downward offset to make access from bottom posible
        box.hideByDefault(true);
        box.setOnRightClick(callback);
        box.spawnEntity();
        return box;
    }
}
