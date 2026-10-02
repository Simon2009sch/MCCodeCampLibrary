package me.simoncrafter.mCCodeCampLibrary.internal.editor;


import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class EditorFrame {
    private final @NotNull AEditor editor;
    private IEditable selection = null;
    private String dialogPath = "";
    private Map<String, Object> currentDialogContext = null;

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

    public void setDialogPath(String dialogPath, Map<String, Object> context) {
        this.dialogPath = dialogPath;
        this.currentDialogContext = context == null ? new HashMap<>() : new HashMap<>(context);
    }

    public Map<String, Object> getDialogContext() {
        return currentDialogContext;
    }

    EditorFrame(@NotNull AEditor editor) throws IllegalArgumentException {
        if (editor == null) {
            throw new NullPointerException("Editor frame was instantiated with a NULL editor!");
        }
        this.editor = editor;
    }
}
