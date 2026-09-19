package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EditorSessionTest {
    @Test void terminatingUnselectedChildReturnsToParent() {
        Player player = mock(Player.class);
        AEditor parent = mock(AEditor.class), child = mock(AEditor.class);
        EditorSession session = new EditorSession(player);
        session.put(parent);
        session.put(child);
        assertDoesNotThrow(() -> session.onEditorTerminate(child));
        assertSame(parent, session.getCurrentEditor());
        verify(child).leave(player);
        assertNull(new EditorSession(player).pop());
        assertFalse(new EditorSession(player).deselect());
    }

    @Test void replacingAndLeavingSelectionsCleansEveryFrame() {
        Player player = mock(Player.class);
        AEditor editor = mock(AEditor.class);
        IEditable first = mock(IEditable.class), second = mock(IEditable.class);
        when(editor.getEditableObjects()).thenReturn(Map.of(UUID.randomUUID(), first, UUID.randomUUID(), second));
        EditorSession session = new EditorSession(player);
        session.put(editor);
        assertTrue(session.select(first));
        assertTrue(session.select(second));
        verify(first).deselect(player);
        session.setEditor(null);
        verify(second).deselect(player);
        assertNull(session.getCurrentEditor());
    }
}
