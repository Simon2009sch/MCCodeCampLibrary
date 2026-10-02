package me.simoncrafter.mCCodeCampLibrary.input.editor;

import me.simoncrafter.mCCodeCampLibrary.internal.editor.EditorManager;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.BlockMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorldMarkerEditorTest {
    @BeforeAll
    static void initializePaperRegistries() {
        MockBukkit.mock();
    }

    @AfterAll
    static void closePaper() {
        MockBukkit.unmock();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> strip(WorldMarkerEditor editor,
                                              Map<String, Object> context,
                                              String dialogKey) throws Exception {
        Method method = WorldMarkerEditor.class.getDeclaredMethod(
                "stripObjectsMeantForDialog", Map.class, String.class);
        method.setAccessible(true);
        return (Map<String, Object>) method.invoke(editor, context, dialogKey);
    }

    @Test
    void dialogContextFiltersOnlyKeysOwnedByTheCurrentDialog() throws Exception {
        WorldMarkerEditor editor = new WorldMarkerEditor(MockBukkit.createMockPlugin());
        Map<String, Object> context = new HashMap<>();
        context.put("purpose", "object_creation");
        context.put("!type_selector_show_home_button", true);
        context.put("!other_dialog_value", "keep");

        Map<String, Object> filtered = strip(editor, context, "type_selector");

        assertEquals("object_creation", filtered.get("purpose"));
        assertEquals("keep", filtered.get("!other_dialog_value"));
        assertFalse(filtered.containsKey("!type_selector_show_home_button"));
        assertEquals(true, context.get("!type_selector_show_home_button"),
                "Filtering must not mutate the caller's context");
    }

    @Test
    void typeSelectionDialogUsesTheCurrentEditorAndRegistryTypes() {
        try (MockedStatic<MCCodeCampLib> lib = mockStatic(MCCodeCampLib.class)) {
            BlockMarkerRegistry registry = mock(BlockMarkerRegistry.class);
            EditorManager manager = mock(EditorManager.class);
            Plugin plugin = MockBukkit.createMockPlugin();
            WorldMarkerEditor editor = new WorldMarkerEditor(plugin);
            Player player = mock(Player.class);

            lib.when(MCCodeCampLib::getBlockMarkerRegistry).thenReturn(registry);
            lib.when(MCCodeCampLib::getEditorManager).thenReturn(manager);
            when(manager.getPlayersCurrentEditor(player)).thenReturn(editor);
            when(registry.getObjectTypes()).thenReturn(Map.of());

            assertDoesNotThrow(() -> editor.displayDialog(player, "type_selector", Map.of(
                    "purpose", "object_creation")));
            verify(registry).getObjectTypes();
        }
    }
}
