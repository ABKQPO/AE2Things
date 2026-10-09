package com.asdflj.ae2thing.client.gui.container;

import net.minecraft.inventory.Slot;

import appeng.api.storage.StorageName;

public interface IPatternValueContainer {

    StorageName getAEStorageName(Slot slot);
}
