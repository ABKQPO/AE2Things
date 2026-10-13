package com.asdflj.ae2thing.common.storage;

import java.util.UUID;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagList;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;

public interface IDataStorage {

    void readFromNBT(NBTTagList data);

    NBTBase writeToNBT();

    IItemList<IAEItemStack> getItems();

    IItemList<IAEFluidStack> getFluids();

    IItemList<IAEStack<?>> getOthers();

    boolean isEmpty();

    String getUUID();

    UUID getRawUUID();

    IAEStackType<?> getStackType();
}
