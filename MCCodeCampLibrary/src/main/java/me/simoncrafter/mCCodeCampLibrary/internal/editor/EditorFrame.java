package me.simoncrafter.mCCodeCampLibrary.internal.editor;


import org.jetbrains.annotations.NotNull;

public class EditorFrame {
    private final @NotNull AEditor editor;
    private IEditable selection = null;

    @NotNull AEditor getEditor() {
        return editor;
    }

    IEditable getSelection() {
        return selection;
    }

    void setSelection(IEditable selection) {
        this.selection = selection;
    }

    EditorFrame(@NotNull AEditor editor) throws IllegalArgumentException {
        if (editor == null) {
            throw new IllegalArgumentException("Editor frame was instantiated with a NULL editor!");
        }
        this.editor = editor;
    }
}
