package me.simoncrafter.mCCodeCampLibrary.input.editor;

import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.GenericQuestion;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.InputActions.StringWithRulesInputAction;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.EditorManager;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.*;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorldMarkerEditorTest {
        @org.junit.jupiter.api.BeforeAll static void initializePaperRegistries() {
        org.mockbukkit.mockbukkit.MockBukkit.mock();
    }

    @org.junit.jupiter.api.AfterAll static void closePaper() {
        org.mockbukkit.mockbukkit.MockBukkit.unmock();
    }

    static Object invoke(Object target, String name, Class<?>[] types, Object... args) {
        return assertDoesNotThrow(() -> {
            var method = target.getClass().getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method.invoke(target, args);
        });
    }
    @Test void deletionRequiresConfirmationAndRechecksTheExactUuid() {
        try (var lib = mockStatic(MCCodeCampLib.class)) {
            BlockMarkerRegistry registry = mock(BlockMarkerRegistry.class);
            EditorManager manager = mock(EditorManager.class);
            lib.when(MCCodeCampLib::getBlockMarkerRegistry).thenReturn(registry);
            lib.when(MCCodeCampLib::getEditorManager).thenReturn(manager);
            WorldMarkerEditor editor = new WorldMarkerEditor(mock(Plugin.class));
            Player player = mock(Player.class);
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(manager.getPlayersCurrentEditor(player)).thenReturn(editor);
            UUID uuid = UUID.randomUUID();
            IBlockRegestryObject marker = mock(IBlockRegestryObject.class);
            when(marker.getUUID()).thenReturn(uuid);
            when(marker.getID()).thenReturn("one");
            when(marker.getTypeID()).thenReturn("test");
            when(registry.findRegisteredObject(uuid)).thenReturn(marker);
            GenericQuestion dialog = (GenericQuestion) invoke(editor, "buildDeleteConfirmation", new Class[]{UUID.class}, uuid);
            verify(registry, never()).removeObject(any(UUID.class));
            dialog.buttons().get(1).onPress(player); // cancel
            verify(registry, never()).removeObject(any(UUID.class));
            dialog = (GenericQuestion) invoke(editor, "buildDeleteConfirmation", new Class[]{UUID.class}, uuid);
            dialog.buttons().get(0).onPress(player);
            verify(registry).removeObject(uuid);
            // A delayed confirmation after unload is harmless.
            when(registry.findRegisteredObject(uuid)).thenReturn(null);
            dialog = (GenericQuestion) invoke(editor, "buildDeleteConfirmation", new Class[]{UUID.class}, uuid);
            dialog.buttons().get(0).onPress(player);
            verify(registry, times(1)).removeObject(uuid);
        }
    }
    @Test void creationUsesRespondingPlayerAndRejectsBlankOrStaleInput() {
        try (var lib = mockStatic(MCCodeCampLib.class)) {
            BlockMarkerRegistry registry = mock(BlockMarkerRegistry.class);
            EditorManager manager = mock(EditorManager.class);
            lib.when(MCCodeCampLib::getBlockMarkerRegistry).thenReturn(registry);
            lib.when(MCCodeCampLib::getEditorManager).thenReturn(manager);
            WorldMarkerEditor editor = new WorldMarkerEditor(mock(Plugin.class));
            Player player = mock(Player.class);
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(manager.getPlayersCurrentEditor(player)).thenReturn(editor);
            Block block = mock(Block.class);
            Location location = new Location(mock(World.class), 1, 64, 2);
            when(block.getLocation()).thenReturn(location);
            when(player.getTargetBlockExact(4, FluidCollisionMode.NEVER)).thenReturn(block);
            var input = (StringWithRulesInputAction) invoke(editor, "buildMarkerIdInput", new Class[]{String.class, Component.class}, "test", Component.text("Test"));
            assertFalse("".matches(input.regexRule()));
            assertTrue(input.reTry(), "Invalid IDs must offer another input rather than strand the player");
            input.onResponse().apply(player).accept("one");
            verify(registry).createObject("test", "one", location);
            when(manager.getPlayersCurrentEditor(player)).thenReturn(null);
            input.onResponse().apply(player).accept("stale");
            verify(registry, never()).createObject(eq("test"), eq("stale"), any());
        }
    }
}
