package me.simoncrafter.mCCodeCampLibrary.internal.editor.area;

import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.CustomAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.MovementQuestion;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.AEditor;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.PlayerClickEditableObjectEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenu.HotbarMenu;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenu.HotbarItem;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import java.util.*;
import java.util.function.*;

/** Reusable marker-relative pivot input; Apply calls back and pops to the caller. */
public final class PivotEditor extends AEditor {
    private final Location origin;
    private final Function<Player, Vector> initial;
    private final BiConsumer<Player, Vector> apply;
    private final Map<UUID, Vector> pivots = new HashMap<>();
    private final Map<UUID, Integer> axes = new HashMap<>();

    /** Creates independent per-player drafts relative to origin; no world blocks are modified. */
    public PivotEditor(Plugin plugin, Location origin, Function<Player, Vector> initial,
                       BiConsumer<Player, Vector> apply) {
        super(plugin);
        this.origin = Objects.requireNonNull(origin, "origin").clone();
        this.initial = Objects.requireNonNull(initial, "initial");
        this.apply = Objects.requireNonNull(apply, "apply");
    }

    /** Creates a pivot input with a fixed initial relative value and an Apply callback. */
    public PivotEditor(Plugin plugin, Location origin, Vector initial, Consumer<Vector> apply) {
        this(plugin, origin, p -> initial.clone(), (p, value) -> apply.accept(value));
    }

    @Override protected void join(Player player) {
        super.join(player);
        pivots.put(player.getUniqueId(), initial.apply(player).clone());
        axes.put(player.getUniqueId(), 0);
    }
    @Override protected void leave(Player player) {
        pivots.remove(player.getUniqueId());
        axes.remove(player.getUniqueId());
        super.leave(player);
    }
    @Override protected void onPlayerClickObjectEvent(PlayerClickEditableObjectEvent event) { }
    @Override public void displayDialog(Player player, String path, Map<String, Object> context) {
        if (active(player)) showPivot(player);
    }
    private boolean active(Player player) {
        return pivots.containsKey(player.getUniqueId())
                && MCCodeCampLib.getEditorManager().getPlayersCurrentEditor(player) == this;
    }
    private void finish(Player player) {
        if (!active(player)) return;
        apply.accept(player, pivots.get(player.getUniqueId()).clone());
        if (active(player)) MCCodeCampLib.getEditorManager().pop(player);
    }
    private HotbarItem item(Material material, String name, boolean unused) {
        ItemStack stack = new ItemStack(material);
        var meta = stack.getItemMeta();
        meta.customName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        stack.setItemMeta(meta);
        HotbarItem item = new HotbarItem(getPlugin(), stack);
        item.setNoInteractMode(material.isBlock() ? HotbarItem.NoInteractMode.Masquerade : HotbarItem.NoInteractMode.NoReach);
        return item;
    }

    private void showPivot(Player player) {
        if (!active(player)) return;

        Vector value = pivots.get(player.getUniqueId());
        MovementQuestion question = MovementQuestion.create().showRotation(false).movementStep(.1)
                .setLocation(value.getX(), value.getY(), value.getZ());
        // MovementQuestion supplies layout/input/reload, while this editor owns the unsaved values.
        question.addPlusXButtonAction(CustomAction.create(p -> change(p, 0, .1, false)));
        question.addMinusXButtonAction(CustomAction.create(p -> change(p, 0, -.1, false)));
        question.addPlusYButtonAction(CustomAction.create(p -> change(p, 1, .1, false)));
        question.addMinusYButtonAction(CustomAction.create(p -> change(p, 1, -.1, false)));
        question.addPlusZButtonAction(CustomAction.create(p -> change(p, 2, .1, false)));
        question.addMinusZButtonAction(CustomAction.create(p -> change(p, 2, -.1, false)));
        question.setSetXButtonAction(p -> v -> change(p, 0, v, true));
        question.setSetYButtonAction(p -> v -> change(p, 1, v, true));
        question.setSetZButtonAction(p -> v -> change(p, 2, v, true));
        question.addExitButtonAction(CustomAction.create(p -> { if (active(p)) finish(p); }));
        question.onReload(p -> { if (pivotActive(p)) showPivot(p); });
        question.show(player, getUUID() + ":pivot:" + player.getUniqueId());

        HotbarMenu menu = new HotbarMenu(getPlugin());
        for (int i = 0; i < 3; i++) {
            int axis = i;
            menu.setItemAt(i, item(Material.COMPASS, "Scroll axis " + "XYZ".charAt(i), false)
                    .addBlockClickAction(e -> {
                        if (!pivotActive(e.getPlayer())) return;
                        axes.put(e.getPlayer().getUniqueId(), axis);
                        menu.setHeldItemSlotQuietly(e.getPlayer(), 4);
                    }));
        }
        menu.setItemAt(4, item(Material.COMPASS, "Scroll pivot; sneak for 0.1 steps", false));
        menu.setItemAt(8, item(Material.BARRIER, "Apply pivot and return", false)
                .addBlockClickAction(e -> { if (pivotActive(e.getPlayer())) finish(e.getPlayer()); }));
        menu.addScrollAction(e -> {
            if (!pivotActive(e.player())) return;
            double step = e.player().isSneaking() ? .1 : 1;
            if (e.direction() != HotbarMenu.ScrollDirection.UP) step = -step;
            change(e.player(), axes.get(e.player().getUniqueId()), step, false);
            // Keep the scroll tool selected without recursively triggering held-slot actions.
            menu.setHeldItemSlotQuietly(e.player(), 4);
            e.player().sendActionBar(net.kyori.adventure.text.Component.text("Pivot: " + pivots.get(e.player().getUniqueId())));
        });
        menu.show(player);
        menu.setHeldItemSlotQuietly(player, 4);
    }

    private boolean pivotActive(Player player) { return active(player); }

    private void change(Player player, int axis, double amount, boolean absolute) {
        if (!pivotActive(player) || !Double.isFinite(amount)) return;
        Vector value = pivots.get(player.getUniqueId());
        double current = axis == 0 ? value.getX() : axis == 1 ? value.getY() : value.getZ();
        double next = absolute ? amount : current + amount;
        if (!Double.isFinite(next)) return;
        if (axis == 0) value.setX(next);
        else if (axis == 1) value.setY(next);
        else value.setZ(next);
    }
}
