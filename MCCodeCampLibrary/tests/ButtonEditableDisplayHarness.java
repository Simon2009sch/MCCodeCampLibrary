package me.simoncrafter.mCCodeCampLibrary.internal.editor.display;

import me.simoncrafter.CraftersDisplayLibrary.core.PositionObject;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.PlayerClickEditableObjectEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.BlockMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.IBlockRegestryObject;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.RegistryObjectType;
import me.simoncrafter.mCCodeCampLibrary.input.activation.ActivationButton;



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

/** Real button + helper code, with entity-free layers and a minimal Bukkit boundary. */
public class ButtonEditableDisplayHarness {
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

    static EditableDisplay displayOf(ActivationButton button) throws Exception {
        Field field = ActivationButton.class.getDeclaredField("editorDisplay");
        field.setAccessible(true);
        return (EditableDisplay) field.get(button);
    }

    public static void main(String[] args) throws Exception {
        List<Event> events = new ArrayList<>();
        PluginManager pm = proxy(PluginManager.class, (p, m, a) -> {
            if (m.getName().equals("callEvent")) events.add((Event) a[0]);
            return null;
        });
        Logger logger = Logger.getLogger("button-display-test");
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
            case "getName" -> "button-display-test";
            case "getLogger" -> logger;
            default -> m.getReturnType() == String.class ? "button-display-test" : null;
        });
        Registry registry = new Registry(plugin);
        RegistryObjectType type = new RegistryObjectType(ActivationButton::new, "simplebutton");
        ActivationButton button = new ActivationButton();
        Location origin = new Location(null, 10, 20, 30);
        button.init(plugin, origin, "entrance", type, BasicConfigurationNode.root(), UUID.randomUUID(), registry);
        EditableDisplay helper = displayOf(button);
        check(helper != null && helper.getChildren().isEmpty(), "init creates a lazy marker");
        Field text = EditableDisplay.class.getDeclaredField("labelText"); text.setAccessible(true);
        check(text.get(helper).equals(Component.text("entrance")), "button ID labels marker");
        List<Layer> normal = new ArrayList<>(), selection = new ArrayList<>(), labels = new ArrayList<>(), interaction = new ArrayList<>();
        List<Consumer<PlayerInteractAtEntityEvent>> callbacks = new ArrayList<>();
        helper.setDisplayCreationMethod(style -> { var layer = new Layer(helper.getLocation()); normal.add(layer); return layer; });
        helper.setLabelCreationMethod(style -> { var layer = new Layer(helper.getLocation()); labels.add(layer); return layer; });
        helper.setSelectionCreationMethod(style -> { var layer = new Layer(helper.getLocation()); selection.add(layer); return layer; });
        helper.setInteractionCreationMethod((style, callback) -> { callbacks.add(callback); var layer = new Layer(helper.getLocation()); interaction.add(layer); return layer; });
        Player player = proxy(Player.class, (p, m, a) -> null);
        Player other = proxy(Player.class, (p, m, a) -> null);
        button.showFor(player);
        check(normal.size() == 1 && normal.getFirst().shows == 1, "show delegates");
        button.select(player);
        check(selection.size() == 1 && normal.getFirst().hides == 0, "select delegates");
        button.deselect(player);
        check(normal.getFirst().shows == 1 && selection.getFirst().hides == 1, "deselect hides only the outline");
        Interaction entity = proxy(Interaction.class, (p, m, a) -> null);
        callbacks.getFirst().accept(new PlayerInteractAtEntityEvent(player, entity, new Vector(), EquipmentSlot.HAND));
        check(events.size() == 1, "exactly one editor click event");
        PlayerClickEditableObjectEvent event = (PlayerClickEditableObjectEvent) events.getFirst();
        check(event.getClicked() == button && event.getPlayer() == player
                && event.getClickType() == PlayerClickEditableObjectEvent.ClickType.RIGHT, "callback targets the button");
        button.setLocation(new Location(null, 40, 50, 60));
        check(helper.getLocation().getX() == 40 && normal.getFirst().getLocation().getX() == 40, "button relocation moves marker and children");
        check(normal.getFirst().getLocalTransform().getTranslation().x == .5f, "relocation retains local center");
        check(registry.saves == 1, "relocation still persists");
        button.showFor(other);
        button.hideFor(player);
        check(normal.getFirst().removes == 0, "remaining viewer keeps marker");
        check(button.getCooldown() == 0, "button cooldown configuration preserved");
        button.getCooldownInstance().setCooldown(3);
        button.getCooldownInstance().start();
        check(button.getCooldownInstance().isOnCooldown(), "button cooldown still starts");
        button.destroy(); button.destroy();
        check(!button.getCooldownInstance().isOnCooldown(), "destruction still disposes cooldown");
        button.getCooldownInstance().start();
        check(!button.getCooldownInstance().isOnCooldown(), "disposed cooldown cannot restart");
        check(helper.getChildren().isEmpty() && normal.getFirst().removes == 1 && interaction.getFirst().removes == 1, "destroy removes marker once");
        check(displayOf(button) == null, "destroy clears helper reference");
        button.showFor(player); button.select(player); button.hideFor(player);
        check(normal.size() == 1, "destroyed button does not respawn marker");
        check(helper.getStyle().equals(new EditableDisplay.Style(org.bukkit.Color.fromARGB(50, 0, 200, 50), org.bukkit.Color.YELLOW, .01f)), "button keeps its green fill and current selection style");
        System.out.println("PASS button integration: green fill + current selection style, lazy label, selection overlay, click event, relocation, shared visibility, cleanup, cooldown");
    }
}
