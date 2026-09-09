package me.simoncrafter.mCCodeCampLibrary.internal.editor;


import org.jetbrains.annotations.NotNull;

public class EditorFrame {
    private final @NotNull Editor editor;
    private IEditable selection = null;

    public @NotNull Editor getEditor() {
        return editor;
    }

    public IEditable getSelection() {
        return selection;
    }

    public void setSelection(IEditable selection) {
        this.selection = selection;
    }

    public EditorFrame(@NotNull Editor editor) throws IllegalArgumentException {
        if (editor == null) {
            throw new IllegalArgumentException("Editor frame was instantiated with a NULL editor!");
        }
        this.editor = editor;
    }
}
