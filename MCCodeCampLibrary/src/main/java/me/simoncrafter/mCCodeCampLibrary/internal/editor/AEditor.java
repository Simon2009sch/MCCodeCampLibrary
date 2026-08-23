package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import me.simoncrafter.CraftersDisplayLibrary.core.interfaces.IDisplayable;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.editables.IEditable;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenue.HotbarItem;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenue.HotbarMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

public abstract class AEditor implements Listener {

    protected static Set<AEditor> editors = new HashSet<>();

    protected final Plugin plugin;
    protected List<Player> players = new ArrayList<>();
    protected Map<IEditable, Set<Player>> editables = new HashMap<>();
    protected HotbarMenu mainHotbarMenu;

    public AEditor(Plugin plugin, List<Player> players) {
        this.plugin = plugin;
        this.players = players;

        mainHotbarMenu = new HotbarMenu(plugin);

        ItemStack exitButton = new ItemStack(Material.BARRIER);
        ItemMeta exitButtonMeta = exitButton.getItemMeta();
        exitButtonMeta.customName(Component.text("Exit Editor", NamedTextColor.RED));
        exitButton.setItemMeta(exitButtonMeta);

        mainHotbarMenu.setItemAt(8, new HotbarItem(plugin, exitButton).addBlockClickAction(e -> {
            this.removePlayer(e.getPlayer());
        }));
        editors.add(this);
    }

    public void closeEditor() {

        editables.forEach((e, p) -> {
            e.getDisplay().remove();
        });
        for (Player p : players) {
            mainHotbarMenu.exit(p);
        }

        editors.remove(this);
    }

    public void addPlayer(Player player) {
        if (players.size() == 1) {
            editables.forEach((e, p) -> {
                e.spawnDisplay();
                e.getDisplay().hideByDefault(true, true);
            });
        }
        players.add(player);
        mainHotbarMenu.show(player);
    }

    public void removePlayer(Player player) {
        players.remove(player);
        mainHotbarMenu.exit(player);
        if (players.isEmpty()) {
            editables.forEach((e, p) -> {
                e.despawnDisplay();
            });
        }
    }

    public void addEditable(IEditable editable) {
        if (editables.containsKey(editable)) {
            return;
        }
        editable.getDisplay().hideByDefault(true, true);
        for (Player p : players) {
            editable.getDisplay().showForPlayer(p);
        }
        editables.put(editable, new HashSet<>());
    }

    public void removeEditable(IEditable editable) {
        if (!editables.containsKey(editable)) {
            return;
        }
        for (Player p : players) {
            editable.getDisplay().hideForPlayer(p);
        }
        editables.remove(editable);
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {

    }

}
