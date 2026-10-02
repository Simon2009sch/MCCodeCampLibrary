package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The Editor session manages the parent child relationship between editors. Provides movement between editors.
 */
// could store other session data in future
public class EditorSession {
    private final Player player;
    private final Deque<EditorFrame> editorStack = new ArrayDeque<>();


    EditorSession(Player player) {
        this.player = player;
    }

    void put(AEditor editor) {
        editor.join(player);
        editorStack.push(new EditorFrame(editor));
    }

    AEditor pop() {
        EditorFrame frame = editorStack.poll();
        if (frame == null) return null;
        if (frame.getSelection() != null) frame.getSelection().deselect(player);
        frame.getEditor().leave(player);

        if (!editorStack.isEmpty()) {
            IEditable sel = editorStack.peek().getSelection();
            if (sel != null) {
                sel.select(player);
            }
        }

        return frame.getEditor();
    }

    /**
     * Removes the players complete editor stack and puts them into the new editor as the root editor
     * @param editor The editor the player is put in. If Null will make player leave editor
     */
    void setEditor(AEditor editor) {
        editorStack.forEach(frame -> {
            if (frame.getSelection() != null) frame.getSelection().deselect(player);
            frame.getEditor().leave(player);
        });
        editorStack.clear();
        if (editor != null) {
            editorStack.push(new EditorFrame(editor));
            editor.join(player);
        }
    }

    @Nullable AEditor getCurrentEditor() {
        return editorStack.peek() != null ? editorStack.peek().getEditor() : null;
    }

    @Nullable IEditable getPlayerSelection() {
        if (editorStack.isEmpty()) return null;
        return editorStack.peek().getSelection();
    }

    IEditable getPlayerSelection(AEditor editor) {
        for (EditorFrame frame : editorStack) {
            if (frame.getEditor() == editor) {
                return frame.getSelection();
            }
        }
        return null;
    }

    boolean select(IEditable editable) {
        EditorFrame frame = editorStack.peek();
        if (frame == null) return false;
        if (frame.getEditor().getEditableObjects().containsValue(editable)) {
            if (frame.getSelection() != null) frame.getSelection().deselect(player);
            frame.setSelection(editable);
            editable.select(player);
            return true;
        }
        return false;
    }

    boolean deselect() {
        EditorFrame frame = editorStack.peek();
        if (frame == null || frame.getSelection() == null) return false;
        frame.getSelection().deselect(player);
        frame.setSelection(null);
        return true;
    }

    /**
     * Moves the player to the editor while safely removing them in all the ones they where in the child-parent tree.
     * @param parent The parent of the editor you want to go to.
     * @param child The target editor you want to go to. If null stays in parent editor
     * @return Returns the number of steps it went outside of an editor
     */
    protected int goTo(AEditor parent, AEditor child) {
        // check if editorframe with parent editor is in stack, else skip
        if (editorStack.isEmpty() || editorStack.stream().noneMatch(e -> e.getEditor() == parent)) {
            return 0;
        }
        int popCount = 0;
        while (editorStack.peek().getEditor() != parent) {
            pop();
            popCount++;
        }
        if (child != null) put(child);
        return popCount;
    }


    void onEditableUnload(UUID uuid) {
        for (EditorFrame frame : editorStack) {
            IEditable selected = frame.getSelection();
            if (selected != null && selected.getUUID().equals(uuid)) {
                selected.deselect(player);
                frame.setSelection(null);
            }
        }
    }

    boolean doesStackContain(AEditor editor) {
        return editorStack.stream().anyMatch(f -> f.getEditor() == editor);
    }

    void onEditorTerminate(AEditor editor) {
        if (editorStack.stream().noneMatch(f -> f.getEditor()==editor)) {
            return;
        }
        while (editorStack.peek().getEditor() != editor) {
            pop();
        }
        pop(); // pop the terminated editor as well
    }

    boolean setDialogPath(String path, Map<String, Object> context) {
        if (editorStack.peek() == null) {
            return false;
        }
        EditorFrame frame = editorStack.peek();
        frame.setDialogPath(path);
        frame.getEditor().displayDialog(player, path, context);
        return true;
    }

    String getDialogPath() {
        EditorFrame frame = editorStack.peek();
        if (frame == null) return "";
        return frame.getDialogPath();
    }

}
