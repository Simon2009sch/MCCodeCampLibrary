package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.PaddingLayout;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The Editor session manages the parent child relationship between editors. Provides movement between editors.
 */
// could store other session data in future
public class EditorSession {
    private final Player player;
    private final Deque<EditorFrame> editorStack = new ArrayDeque<>();

    public EditorSession(Player player) {
        this.player = player;
    }

    public void put(Editor editor) {
        editor.join(player);
        editorStack.push(new EditorFrame(editor));
    }

    public Editor pop() {
        EditorFrame frame = editorStack.pop();
        frame.getEditor().leave(player);
        frame.getSelection().hideSelection(player);

        if (!editorStack.isEmpty()) {
            IEditable sel = editorStack.peek().getSelection();
            if (sel != null) {
                sel.showSelection(player);
            }
        }

        return frame.getEditor();
    }

    public @Nullable IEditable getPlayerSelection() {
        if (editorStack.isEmpty()) return null;
        return editorStack.peek().getSelection();
    }

    public IEditable getPlayerSelection(Editor editor) {
        for (EditorFrame frame : editorStack) {
            if (frame.getEditor() == editor) {
                return frame.getSelection();
            }
        }
        return null;
    }

    /**
     * Moves the player to the editor while safely removing them in all the ones they where in the child-parent tree.
     * @param parent The parent of the editor you want to go to.
     * @param target The target editor you want to go to. If null stays in parent editor
     * @return Returns the number of steps it went outside of an editor
     */
    public int goTo(Editor parent, Editor target) {
        // check if editorframe with parent editor is in stack, else skip
        if (editorStack.isEmpty() || editorStack.stream().noneMatch(e -> e.getEditor() == parent)) {
            return 0;
        }
        int popCount = 0;
        while (editorStack.peek().getEditor() != parent) {
            editorStack.pop().getEditor().leave(player);
            popCount++;
        }
        if (target != null) put(target);
        return popCount;
    }

}
