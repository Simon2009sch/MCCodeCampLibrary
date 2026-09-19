package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.EditorTerminateEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.IBlockRegestryObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EditorLifecycleTest {
    private static me.simoncrafter.mCCodeCampLibrary.internal.registry.events.BlockRegistryUpdateEvent unloadEvent(UUID uuid) {
        IBlockRegestryObject marker = mock(IBlockRegestryObject.class);
        when(marker.getUUID()).thenReturn(uuid);
        return new me.simoncrafter.mCCodeCampLibrary.internal.registry.events.BlockRegistryUpdateEvent(
                me.simoncrafter.mCCodeCampLibrary.internal.registry.events.BlockRegistryUpdateEvent.UpdateType.UNLOAD,
                marker
        );
    }

    @Test void ownedEditorIsTerminatedButSharedParentSurvives() throws Exception {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            PluginManager plugins = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
            EditorManager manager = new EditorManager(mock(Plugin.class));
            AEditor parent = mock(AEditor.class), child = mock(AEditor.class);
            IEditable owner = mock(IEditable.class);
            UUID uuid = UUID.randomUUID();
            when(owner.getUUID()).thenReturn(uuid);
            manager.registerEditor("parent", parent);
            var registration = assertDoesNotThrow(() -> EditorManager.class.getMethod("registerEditor", String.class, AEditor.class, IEditable.class));
            assertEquals(true, registration.invoke(manager, "child", child, owner));
            Player player = mock(Player.class);
            manager.setEditor(player, parent);
            manager.put(player, child);
            manager.onBlockRegistryUpdate(unloadEvent(uuid));
            assertSame(parent, manager.getPlayersCurrentEditor(player));
            assertFalse(manager.put(player, child));
            verify(child).leave(player);
            manager.onBlockRegistryUpdate(unloadEvent(uuid));
            verify(child, times(1)).leave(player);
        }
    }

    @Test void unloadRemovesSelectionWithoutTerminatingSharedEditor() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            PluginManager plugins = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
            EditorManager manager = new EditorManager(mock(Plugin.class));
            AEditor shared = new AEditor(mock(Plugin.class)) {
                protected void onPlayerClickObjectEvent(me.simoncrafter.mCCodeCampLibrary.internal.editor.events.PlayerClickEditableObjectEvent e) {}
            };
            IEditable marker = mock(IEditable.class);
            UUID uuid = UUID.randomUUID();
            when(marker.getUUID()).thenReturn(uuid);
            shared.addEditableObject(marker);
            manager.registerEditor("shared", shared);
            Player one = mock(Player.class), two = mock(Player.class);
            manager.setEditor(one, shared);
            manager.setEditor(two, shared);
            manager.select(one, marker);
            // Deliver the existing lifecycle event exactly as Bukkit does.
            Event event = unloadEvent(uuid);
            for (var method : manager.getClass().getMethods()) {
                if (method.isAnnotationPresent(org.bukkit.event.EventHandler.class)
                        && method.getParameterTypes()[0].isInstance(event)) {
                    try { method.invoke(manager, event); } catch (Exception ex) { throw new AssertionError(ex); }
                }
            }
            assertNull(manager.getPlayerSelection(one));
            assertFalse(shared.getEditableObjects().containsKey(uuid));
            assertSame(shared, manager.getPlayersCurrentEditor(one));
            assertSame(shared, manager.getPlayersCurrentEditor(two));
            verify(marker, atLeastOnce()).hideFor(any(Player.class));
            manager.leaveEditor(one);
            assertDoesNotThrow(() -> manager.leaveEditor(mock(Player.class)));
        }
    }
}
