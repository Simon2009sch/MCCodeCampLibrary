package me.simoncrafter.mCCodeCampLibrary.internal.registry;

import me.simoncrafter.mCCodeCampLibrary.input.editor.WorldMarkerEditor;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.*;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.PlayerClickEditableObjectEvent;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import org.bukkit.Location;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EditorRegistryIntegrationTest {
    @Test void registryEventsUpdateBothViewersAndTerminateOnlyOwnedEditor() {
        var server = MockBukkit.mock();
        try (var lib = mockStatic(MCCodeCampLib.class)) {
            var plugin = MockBukkit.createMockPlugin();
            var registry = new BlockMarkerRegistry(plugin);
            registry.registerObjectType("test", new RegistryObjectType(() -> spy(new BlockMarkerLifecycleTest.Marker()), "test"));
            var manager = new EditorManager(plugin);
            server.getPluginManager().registerEvents(registry, plugin);
            lib.when(MCCodeCampLib::getBlockMarkerRegistry).thenReturn(registry);
            lib.when(MCCodeCampLib::getEditorManager).thenReturn(manager);
            var editor = new WorldMarkerEditor(plugin);
            manager.registerEditor("world", editor);
            var one = server.addPlayer();
            var two = server.addPlayer();
            manager.setEditor(one, editor);
            manager.setEditor(two, editor);
            var world = server.addSimpleWorld("markers");
            var location = new Location(world, 1, 64, 1);
            var marker = (BlockMarkerLifecycleTest.Marker) registry.createObject("test", "one", location);
            verify(marker).showFor(one);
            verify(marker).showFor(two);
            assertTrue(manager.select(one, marker));
            AEditor child = new AEditor(plugin) {
                protected void onPlayerClickObjectEvent(PlayerClickEditableObjectEvent event) {}
            };
            manager.registerEditor("owned", child, marker);
            manager.put(one, child);
            server.getPluginManager().callEvent(new ChunkUnloadEvent(location.getChunk()));
            assertSame(editor, manager.getPlayersCurrentEditor(one));
            assertSame(editor, manager.getPlayersCurrentEditor(two));
            assertFalse(manager.put(one, child));
            assertNull(manager.getPlayerSelection(one));
            assertFalse(editor.getEditableObjects().containsKey(marker.getUUID()));
            assertNull(registry.findRegisteredObject(marker.getUUID()));
            verify(marker).hideFor(one);
            verify(marker).hideFor(two);
            registry.onChunkLoad(location.getChunk());
            var loaded = registry.findRegisteredObject(marker.getUUID());
            assertNotNull(loaded);
            assertSame(loaded, editor.getEditableObjects().get(marker.getUUID()));
            registry.removeObject(loaded);
            assertFalse(editor.getEditableObjects().containsKey(marker.getUUID()));
            assertSame(editor, manager.getPlayersCurrentEditor(two));
            manager.leaveEditor(one);
            manager.leaveEditor(two);
        } finally { MockBukkit.unmock(); }
    }
}
