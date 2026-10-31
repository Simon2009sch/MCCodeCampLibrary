package me.simoncrafter.mCCodeCampLibrary.internal.editor.display;

import me.simoncrafter.CraftersDisplayLibrary.core.PositionObject;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Exercises the real helper and CDL transform hierarchy without starting a Paper server. */
public class PositionEditableDisplayHarness {
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static class Layer extends PositionObject {
        int shows, hides, removals;
        Layer(Location origin, Vector3f offset) {
            super(List.of(), new Transformation(offset, new Quaternionf(), new Vector3f(1), new Quaternionf()), origin);
        }
        @Override public PositionObject showForPlayer(Player player, boolean recursive) { shows++; return this; }
        @Override public PositionObject hideForPlayer(Player player, boolean recursive) { hides++; return this; }
        @Override public void remove() { removals++; }
    }

    @SuppressWarnings("unchecked")
    static <T> T dummy(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, m, a) -> switch (m.getName()) {
            case "hashCode" -> System.identityHashCode(p);
            case "equals" -> p == a[0];
            case "toString", "getName" -> "test";
            default -> null;
        });
    }

    public static void main(String[] args) {
        Location original = new Location(null, 10, 20, 30);
        EditableDisplay helper = new EditableDisplay(original, dummy(Plugin.class));
        check(helper instanceof PositionObject, "helper itself must be the parent");
        List<Layer> normals = new ArrayList<>(), labels = new ArrayList<>(), selections = new ArrayList<>(), interactions = new ArrayList<>();
        List<Consumer<PlayerInteractAtEntityEvent>> callbacks = new ArrayList<>();
        Consumer<PlayerInteractAtEntityEvent> callback = event -> {};
        helper.setOnRightClick(callback);
        helper.setDisplayCreationMethod(style -> { var layer = new Layer(helper.getLocation(), new Vector3f(1, 0, 0)); normals.add(layer); return layer; });
        helper.setLabelCreationMethod(style -> { var layer = new Layer(helper.getLocation(), new Vector3f(0, 2, 0)); labels.add(layer); return layer; });
        helper.setSelectionCreationMethod(style -> { var layer = new Layer(helper.getLocation(), new Vector3f()); selections.add(layer); return layer; });
        helper.setInteractionCreationMethod((style, click) -> { callbacks.add(click); var layer = new Layer(helper.getLocation(), new Vector3f(0, 0, 3)); interactions.add(layer); return layer; });
        check(helper.getChildren().isEmpty(), "construction must be lazy");
        Player a = dummy(Player.class), b = dummy(Player.class);
        helper.showFor(a);
        check(normals.size() == 1 && labels.size() == 1 && interactions.size() == 1, "create all ordinary layers");
        check(selections.isEmpty(), "selection must not spawn before selecting");
        check(callbacks.getFirst() == callback, "callback must be passed through unchanged");
        check(helper.getChildren().size() == 3, "all layers belong directly to helper");
        helper.showFor(a);
        check(normals.getFirst().shows == 1, "duplicate show must not recreate or reshow layers");
        helper.select(a);
        check(selections.size() == 1 && normals.getFirst().hides == 0, "selection overlays the normal cube");
        helper.showFor(b);
        check(normals.size() == 1 && normals.getFirst().shows == 2, "another viewer uses same layers");
        helper.deselect(a);
        check(selections.getFirst().hides >= 1 && normals.getFirst().shows == 2, "deselection hides only the outline");
        // Existing PositionObject methods must move and transform children without helper overrides.
        helper.setLocation(new Location(null, 40, 50, 60));
        check(normals.getFirst().getLocation().getX() == 40, "setLocation moves children");
        check(normals.getFirst().getLocalTransform().getTranslation().x == 1, "local offset survives relocation");
        helper.scaleAbsolute(new Vector3f(2), 0);
        check(normals.getFirst().getFinalTransform().getTranslation().x == 2, "parent scale affects offset");
        check(labels.getFirst().getFinalTransform().getTranslation().y == 4, "label uses parent transform");
        helper.select(b);
        helper.rebuild();
        check(normals.getLast().shows == 2, "rebuild restores the cube for all viewers, including selected viewers");
        check(normals.getFirst().removals == 1 && labels.getFirst().removals == 1 && interactions.getFirst().removals == 1, "rebuild removes previous layers");
        check(helper.getChildren().size() == 4, "rebuild must not retain removed children");
        check(selections.size() == 2, "selected viewers are restored after rebuild");
        helper.hideFor(a);
        check(normals.getLast().removals == 0, "one viewer leaving must not remove shared layers");
        helper.hideFor(b);
        check(helper.getChildren().isEmpty(), "last viewer removes and detaches every layer");
        helper.select(a);
        check(helper.getChildren().isEmpty(), "selecting a hidden object does not spawn it");
        helper.showFor(a);
        check(selections.size() == 3, "selection is remembered before show");
        helper.remove(); helper.remove();
        check(helper.getChildren().isEmpty() && normals.getLast().removals == 1, "remove is repeat-safe");
        original.add(100, 0, 0);
        check(helper.getLocation().getX() == 40, "input location is not retained by reference");
        System.out.println("PASS: CDL parent transforms, layer factories, direct callback forwarding, lazy selection, per-viewer switching, rebuild and cleanup");
    }
}
