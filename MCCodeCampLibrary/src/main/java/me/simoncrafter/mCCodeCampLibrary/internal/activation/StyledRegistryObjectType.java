package me.simoncrafter.mCCodeCampLibrary.internal.activation;

import me.simoncrafter.mCCodeCampLibrary.internal.editor.IEditorObjectDescriptor;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.IBlockRegestryObject;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.RegistryObjectType;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Extention of registry object type made
 */
public class StyledRegistryObjectType extends RegistryObjectType implements IEditorObjectDescriptor {

    private Map<String, String> displayValues;

    @Override
    public @NotNull String getDisplayKey(String key) {
        return displayValues.getOrDefault(key, "Component.empty()");
    }

    public StyledRegistryObjectType(Callable<IBlockRegestryObject> createObjectAction, String typeID, Map<String, String> displayValues) {
        super(createObjectAction, typeID);
        this.displayValues = displayValues;
    }


    public Map<String, String> getDisplayValues() {
        return displayValues;
    }

    public void setDisplayValues(Map<String, String> displayValues) {
        this.displayValues = displayValues;
    }
}
