package me.simoncrafter.mCCodeCampLibrary.internal.editor;


import org.jetbrains.annotations.NotNull;

public class EditorFrame {
    private final @NotNull AEditor editor;
    private IEditable selection = null;
    private String dialogPath = "";

    @NotNull AEditor getEditor() {
        return editor;
    }

    IEditable getSelection() {
        return selection;
    }

    void setSelection(IEditable selection) {
        this.selection = selection;
    }

    public String getDialogPath() {
        return dialogPath;
    }

    public void setDialogPath(String dialogPath) {
        this.dialogPath = dialogPath;
    }

    EditorFrame(@NotNull AEditor editor) throws IllegalArgumentException {
        if (editor == null) {
            throw new NullPointerException("Editor frame was instantiated with a NULL editor!");
        }
        this.editor = editor;
    }
}
