package me.simoncrafter.mCCodeCampLibrary.internal.editor.display;

import me.simoncrafter.CraftersDisplayLibrary.core.PositionObject;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.PlayerClickEditableObjectEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.BlockMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.IBlockRegestryObject;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.RegistryObjectType;
import me.simoncrafter.mCCodeCampLibrary.obstical.AOpenableObject;
import me.simoncrafter.mCCodeCampLibrary.obstical.Door.SimpleDoor;
import me.simoncrafter.mCCodeCampLibrary.obstical.Door.PivotingDoor;
import me.simoncrafter.mCCodeCampLibrary.obstical.Door.SlidingDoor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.configurate.BasicConfigurationNode;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

/** Real door + helper code, with entity-free layers and a minimal Bukkit boundary. */
public class DoorEditableDisplayHarness {
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, m, a) -> {
            if (m.getName().equals("hashCode")) return System.identityHashCode(p);
            if (m.getName().equals("equals")) return p == a[0];
            if (m.getName().equals("toString")) return type.getSimpleName();
            return handler.invoke(p, m, a);
        });
    }

    static class Registry extends BlockMarkerRegistry {
        int saves;
        Registry(Plugin plugin) { super(plugin); }
        @Override public void saveObject(IBlockRegestryObject object) { saves++; }
    }

    static class Layer extends PositionObject {
        int shows, hides, removes;
        Layer(Location location) {
            super(List.of(), new Transformation(new Vector3f(.5f), new Quaternionf(), new Vector3f(1), new Quaternionf()), location);
        }
        @Override public PositionObject showForPlayer(Player player, boolean recursive) { shows++; return this; }
        @Override public PositionObject hideForPlayer(Player player, boolean recursive) { hides++; return this; }
        @Override public void remove() { removes++; }
    }

    static EditableDisplay displayOf(AOpenableObject door) throws Exception {
        Field field = AOpenableObject.class.getDeclaredField("editorDisplay");
        field.setAccessible(true);
        return (EditableDisplay) field.get(door);
    }

    public static void main(String[] args) throws Exception {
        List<Event> events = new ArrayList<>();
        PluginManager pm = proxy(PluginManager.class, (p, m, a) -> {
            if (m.getName().equals("callEvent")) events.add((Event) a[0]);
            return null;
        });
        Logger logger = Logger.getLogger("door-display-test");
        Server server = proxy(Server.class, (p, m, a) -> switch (m.getName()) {
            case "getWorlds" -> List.of();
            case "isPrimaryThread" -> true;
            case "getName", "getVersion", "getBukkitVersion" -> "test";
            case "getLogger" -> logger;
            case "getPluginManager" -> pm;
            default -> null;
        });
        try { Bukkit.setServer(server); }
        catch (NoSuchElementException apiOnlyBuildInfo) { check(Bukkit.getServer() == server, "server installed"); }
        Plugin plugin = proxy(Plugin.class, (p, m, a) -> switch (m.getName()) {
            case "getName" -> "door-display-test";
            case "getLogger" -> logger;
            default -> m.getReturnType() == String.class ? "door-display-test" : null;
        });
        Registry registry = new Registry(plugin);
        RegistryObjectType type = new RegistryObjectType(SimpleDoor::new, "simpledoor");
        SimpleDoor door = new SimpleDoor();
        Location origin = new Location(null, 10, 20, 30);
        door.init(plugin, origin, "entrance", type, BasicConfigurationNode.root(), UUID.randomUUID(), registry);
        EditableDisplay helper = displayOf(door);
        check(helper != null && helper.getChildren().isEmpty(), "init creates a lazy marker");
        Field text = EditableDisplay.class.getDeclaredField("labelText"); text.setAccessible(true);
        check(text.get(helper).equals(Component.text("entrance")), "door ID labels marker");
        List<Layer> normal = new ArrayList<>(), selection = new ArrayList<>(), labels = new ArrayList<>(), interaction = new ArrayList<>();
        List<Consumer<PlayerInteractAtEntityEvent>> callbacks = new ArrayList<>();
        helper.setDisplayCreationMethod(style -> { var layer = new Layer(helper.getLocation()); normal.add(layer); return layer; });
        helper.setLabelCreationMethod(style -> { var layer = new Layer(helper.getLocation()); labels.add(layer); return layer; });
        helper.setSelectionCreationMethod(style -> { var layer = new Layer(helper.getLocation()); selection.add(layer); return layer; });
        helper.setInteractionCreationMethod((style, callback) -> { callbacks.add(callback); var layer = new Layer(helper.getLocation()); interaction.add(layer); return layer; });
        Player player = proxy(Player.class, (p, m, a) -> null);
        Player other = proxy(Player.class, (p, m, a) -> null);
        door.showFor(player);
        check(normal.size() == 1 && normal.getFirst().shows == 1, "show delegates");
        door.select(player);
        check(selection.size() == 1 && normal.getFirst().hides == 0, "select overlays the cube");
        door.deselect(player);
        check(normal.getFirst().shows == 1 && selection.getFirst().hides == 1, "deselect hides only the outline");
        Interaction entity = proxy(Interaction.class, (p, m, a) -> null);
        callbacks.getFirst().accept(new PlayerInteractAtEntityEvent(player, entity, new Vector(), EquipmentSlot.HAND));
        check(events.size() == 1, "exactly one editor click event");
        PlayerClickEditableObjectEvent event = (PlayerClickEditableObjectEvent) events.getFirst();
        check(event.getClicked() == door && event.getPlayer() == player
                && event.getClickType() == PlayerClickEditableObjectEvent.ClickType.RIGHT, "callback targets the door");
        door.setLocation(new Location(null, 40, 50, 60));
        check(helper.getLocation().getX() == 40 && normal.getFirst().getLocation().getX() == 40, "door relocation moves marker and children");
        check(normal.getFirst().getLocalTransform().getTranslation().x == .5f, "relocation retains local center");
        check(registry.saves == 1, "relocation still persists");
        door.showFor(other);
        door.hideFor(player);
        check(normal.getFirst().removes == 0, "remaining viewer keeps marker");
        door.destroy(); door.destroy();
        check(helper.getChildren().isEmpty() && normal.getFirst().removes == 1 && interaction.getFirst().removes == 1, "destroy removes marker once");
        check(displayOf(door) == null, "destroy clears helper reference");
        door.showFor(player); door.select(player); door.hideFor(player);
        check(normal.size() == 1, "destroyed door does not respawn marker");
        // Loaded doors normally have a saved block-region list even without region visuals.
        for (AOpenableObject candidate : List.of(new SimpleDoor(), new PivotingDoor(), new SlidingDoor())) {
            var config = BasicConfigurationNode.root();
            var region = config.node("blocks").appendListNode();
            region.node("min", "x").set(0);
            region.node("max", "x").set(1);
            candidate.init(plugin, origin.clone(), candidate.getClass().getSimpleName(), type,
                    config, UUID.randomUUID(), registry);
            check(candidate.getBlocks().size() == 2, "saved block regions still load");
            EditableDisplay marker = displayOf(candidate);
            List<Layer> layers = new ArrayList<>();
            marker.setDisplayCreationMethod(style -> { var layer = new Layer(marker.getLocation()); layers.add(layer); return layer; });
            marker.setLabelCreationMethod(style -> new Layer(marker.getLocation()));
            marker.setSelectionCreationMethod(style -> new Layer(marker.getLocation()));
            marker.setInteractionCreationMethod((style, callback) -> new Layer(marker.getLocation()));
            candidate.showFor(player);
            candidate.select(player);
            candidate.deselect(player);
            candidate.setLocation(new Location(null, 7, 8, 9));
            check(marker.getLocation().getY() == 8, "all openable types relocate their marker");
            candidate.hideFor(player);
            check(marker.getChildren().isEmpty(), "all openable types hide and release their marker");
            candidate.showFor(player);
            candidate.destroy();
            check(marker.getChildren().isEmpty() && layers.getLast().removes == 1, "animated destroy chain also removes marker");
        }
        System.out.println("PASS door integration: all openable types, saved blocks, lazy ID marker, editor delegation, right-click event, relocation + persistence, shared viewers, destruction");
    }
}
