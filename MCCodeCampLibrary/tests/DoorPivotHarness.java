package me.simoncrafter.mCCodeCampLibrary.internal.editor.display;

import java.util.*;
import java.util.logging.Logger;
import org.bukkit.*;
import org.bukkit.plugin.*;
import org.bukkit.util.Vector;
import org.spongepowered.configurate.BasicConfigurationNode;
import me.simoncrafter.mCCodeCampLibrary.obstical.Door.SlidingDoor;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.RegistryObjectType;

/** Production pivot persistence and defensive-copy checks at the Bukkit boundary. */
public class DoorPivotHarness {
    public static void main(String[] args) throws Exception {
        PluginManager pm = DoorEditableDisplayHarness.proxy(PluginManager.class, (p,m,a) -> null);
        Server server = DoorEditableDisplayHarness.proxy(Server.class, (p,m,a) -> switch(m.getName()) {
            case "getPluginManager" -> pm;
            case "getWorlds" -> List.of();
            case "getLogger" -> Logger.getLogger("pivot-test");
            case "getName", "getVersion", "getBukkitVersion" -> "test";
            default -> null;
        });
        try { Bukkit.setServer(server); }
        catch (NoSuchElementException expected) { if (Bukkit.getServer() != server) throw expected; }
        Plugin plugin = DoorEditableDisplayHarness.proxy(Plugin.class, (p,m,a) ->
                m.getReturnType() == String.class ? "pivot-test" : null);
        var registry = new DoorEditableDisplayHarness.Registry(plugin);
        var config = BasicConfigurationNode.root();
        SlidingDoor door = new SlidingDoor();
        door.init(plugin, new Location(null, 100, 20, 30), "test", new RegistryObjectType(SlidingDoor::new,"slidingdoor"), config, UUID.randomUUID(), registry);
        assert door.getPivotPosition().equals(new Vector(.5,.5,.5)) : "existing relative default retained";
        Vector supplied = new Vector(-2, .25, 3);
        door.setPivotPosition(supplied);
        supplied.setX(99);
        assert door.getPivotPosition().getX() == -2 : "setter owns input";
        Vector returned = door.getPivotPosition(); returned.setY(99);
        assert door.getPivotPosition().getY() == .25 : "getter detached";
        assert config.node("pivotpoint", "x").getDouble() == -2 : "config updated";
        assert registry.saves == 1 : "pivot persisted";
        try { door.setPivotPosition(new Vector(Double.NaN, 0, 0)); throw new AssertionError("NaN accepted"); }
        catch (IllegalArgumentException expected) { }
        assert registry.saves == 1 : "invalid pivot never persisted";
        System.out.println("DoorPivotHarness: 6 behavior checks passed");
    }
}
