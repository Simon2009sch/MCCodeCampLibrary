package me.simoncrafter.mCCodeCampLibrary.internal.registry;

import java.util.concurrent.Callable;

public class RegistryObjectType {
    private String typeID;
    private Callable<IBlockRegestryObject> createObjectAction;

    public RegistryObjectType(Callable<IBlockRegestryObject> createObjectAction, String typeID) {
        this.createObjectAction = createObjectAction;
        this.typeID = typeID;
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

    void setCreateObjectAction(Callable<IBlockRegestryObject> createObjectAction) {
        this.createObjectAction = createObjectAction;
    }
}
