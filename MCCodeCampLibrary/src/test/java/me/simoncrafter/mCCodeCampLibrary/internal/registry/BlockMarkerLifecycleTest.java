package me.simoncrafter.mCCodeCampLibrary.internal.registry;

import me.simoncrafter.mCCodeCampLibrary.internal.editor.IEditable;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.events.BlockRegistryUpdateEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.persistence.*;
import org.bukkit.plugin.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.spongepowered.configurate.ConfigurationNode;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlockMarkerLifecycleTest {
    @Test void createUnloadReloadDeleteKeepsPersistenceAndPublishesEvents() {
        try (Fixture f = new Fixture()) {
            var marker = f.registry.createObject("test", "one", f.location);
            assertNotNull(marker);
            assertTrue(f.events.contains(BlockRegistryUpdateEvent.UpdateType.CREATE));
            String persisted = f.data.get();
            assertNotNull(persisted);
            f.events.clear();
            f.registry.onChunkUnload(f.chunk);
            assertNull(f.registry.findRegisteredObject(marker.getUUID()));
            assertEquals(persisted, f.data.get());
            assertEquals(List.of(BlockRegistryUpdateEvent.UpdateType.UNLOAD), f.events);
            f.registry.onChunkLoad(f.chunk);
            var loaded = f.registry.findRegisteredObject(marker.getUUID());
            assertNotNull(loaded);
            assertNotSame(marker, loaded);
            assertTrue(f.events.contains(BlockRegistryUpdateEvent.UpdateType.LOAD));
            f.registry.removeObject(loaded);
            assertNull(f.registry.findRegisteredObject(marker.getUUID()));
            assertFalse(f.data.get().contains(marker.getUUID().toString()));
            assertTrue(f.events.contains(BlockRegistryUpdateEvent.UpdateType.DELETE));
        }
    }
    @Test void duplicateCreationNeverOverwritesRuntimeOrPersistence() {
        try (Fixture f = new Fixture()) {
            var marker = f.registry.createObject("test", "same", f.location);
            String persisted = f.data.get();
            assertNull(f.registry.createObject("test", "same", f.location));
            assertSame(marker, f.registry.findRegisteredObject("test", "same"));
            assertEquals(persisted, f.data.get());
        }
    }
    @Test void directDestroyOnlyReleasesAttachments() {
        try (Fixture f = new Fixture()) {
            var marker = f.registry.createObject("test", "one", f.location);
            f.events.clear();
            marker.destroy();
            assertTrue(f.events.isEmpty());
        }
    }
    static class Fixture implements AutoCloseable {
        final MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        final World world = mock(World.class);
        final Chunk chunk = mock(Chunk.class);
        final AtomicReference<String> data = new AtomicReference<>();
        final List<BlockRegistryUpdateEvent.UpdateType> events = new ArrayList<>();
        final BlockMarkerRegistry registry;
        final Location location;
        Fixture() {
            Plugin plugin = mock(Plugin.class);
            when(plugin.getName()).thenReturn("test");
            when(plugin.namespace()).thenReturn("test");
            when(plugin.getLogger()).thenReturn(Logger.getLogger("marker-test"));
            PluginManager plugins = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of());
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
            doAnswer(call -> { if (call.getArgument(0) instanceof BlockRegistryUpdateEvent e) events.add(e.getUpdateType()); return null; }).when(plugins).callEvent(any());
            when(world.getName()).thenReturn("world");
            when(world.getChunkAt(anyInt(), anyInt())).thenReturn(chunk);
            when(world.getChunkAt(any(Location.class))).thenReturn(chunk);
            when(chunk.getWorld()).thenReturn(world);
            PersistentDataContainer pdc = mock(PersistentDataContainer.class);
            when(chunk.getPersistentDataContainer()).thenReturn(pdc);
            when(pdc.get(any(NamespacedKey.class), eq(PersistentDataType.STRING))).thenAnswer(call -> data.get());
            doAnswer(call -> { data.set(call.getArgument(2)); return null; }).when(pdc).set(any(NamespacedKey.class), eq(PersistentDataType.STRING), anyString());
            location = new Location(world, 1, 64, 1);
            registry = new BlockMarkerRegistry(plugin);
            registry.registerObjectType("test", new RegistryObjectType(Marker::new, "test"));
        }
        public void close() { bukkit.close(); }
    }
    static class Marker implements IBlockRegestryObject, IEditable {
        UUID uuid; String id; Location location; ConfigurationNode config;
        public void init(Plugin plugin, Location loc, String id, RegistryObjectType type, ConfigurationNode config, UUID uuid, BlockMarkerRegistry registry) {
            this.uuid = uuid; this.id = id; this.location = loc; this.config = config;
        }
        public UUID getUUID() { return uuid; }
        public String getID() { return id; }
        public String getTypeID() { return "test"; }
        public Location getLocation() { return location; }
        public void setLocation(Location location) { this.location = location; }
        public ConfigurationNode getConfig() { return config; }
        public void showFor(Player player) {}
        public void hideFor(Player player) {}
        public void select(Player player) {}
        public void deselect(Player player) {}
    }
}
