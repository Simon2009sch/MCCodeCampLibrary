package me.simoncrafter.mCCodeCampLibrary.internal.registry;

import java.util.concurrent.Callable;

public record RegistryObjectType(String typeID, Callable<IBlockRegestryObject> createObject) {}
