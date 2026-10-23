package com.asdflj.ae2thing.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import com.asdflj.ae2thing.api.Constants;

import appeng.api.storage.data.IAEStack;

public class CPUCraftingPreview {

    public final String name;
    public final long remainingItemCount;
    public final List<IAEStack<?>> itemList;
    public final long elapsedTime;
    public static final int maxSize = 5;

    public CPUCraftingPreview(String name, long remainingItemCount, long elapsedTime, List<IAEStack<?>> list) {
        this.name = name;
        this.remainingItemCount = remainingItemCount;
        this.itemList = list;
        this.elapsedTime = elapsedTime;
    }

    public void writeToNBT(NBTTagCompound data) {
        data.setString(Constants.NAME, this.name);
        data.setLong(Constants.REMAINING_ITEM_COUNT, this.remainingItemCount);
        data.setLong(Constants.CPU_ELAPSED_TIME, this.elapsedTime);
        final List<IAEStack<?>> items = new ArrayList<>();
        for (IAEStack<?> aes : this.itemList) if (aes != null) items.add(aes);
        data.setInteger(Constants.SIZE, items.size());
        for (int i = 0; i < items.size(); i++) {
            IAEStack<?> item = items.get(i);
            NBTTagCompound tag = new NBTTagCompound();
            item.writeToNBTGeneric(tag);
            data.setTag("#" + i, tag);
        }
    }

    public static CPUCraftingPreview readFromNBT(NBTTagCompound data) {
        String name = data.getString(Constants.NAME);
        long remainingItemCount = data.getLong(Constants.REMAINING_ITEM_COUNT);
        long elapsedTime = data.getLong(Constants.CPU_ELAPSED_TIME);
        int size = data.getInteger(Constants.SIZE);
        List<IAEStack<?>> itemList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            NBTTagCompound tag = data.getCompoundTag("#" + i);
            IAEStack<?> aes = IAEStack.fromNBTGeneric(tag);
            if (aes != null) itemList.add(aes);
        }
        return new CPUCraftingPreview(name, remainingItemCount, elapsedTime, itemList);
    }
}
