package me.simoncrafter.mCCodeCampLibrary.internal.registry;

import java.util.concurrent.Callable;

public class RegistryObjectType {
    private String typeID;
    private Callable<IBlockRegestryObject> createObjectAction;
    private boolean allowMultipleOfSameID = false;

    public RegistryObjectType(Callable<IBlockRegestryObject> createObjectAction, String typeID) {
        this.createObjectAction = createObjectAction;
        this.typeID = typeID;
    }

    public RegistryObjectType(Callable<IBlockRegestryObject> createObjectAction, String typeID, boolean allowMultipleOfSameID) {
        this.createObjectAction = createObjectAction;
        this.typeID = typeID;
        this.allowMultipleOfSameID = allowMultipleOfSameID;
    }

    public String getTypeID() {
        return typeID;
    }

    void setTypeID(String typeID) {
        this.typeID = typeID;
    }

    public Callable<IBlockRegestryObject> getCreateObjectAction() {
        return createObjectAction;
    }

    public void setAllowMultipleOfSameID(boolean allowMultipleOfSameID) {
        this.allowMultipleOfSameID = allowMultipleOfSameID;
    }

    boolean allowMultipleOfSameID() {
        return allowMultipleOfSameID;
    }

    void setCreateObjectAction(Callable<IBlockRegestryObject> createObjectAction) {
        this.createObjectAction = createObjectAction;
    }
}
