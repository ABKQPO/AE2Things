package com.asdflj.ae2thing.common.storage;

import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;

public interface RefreshableStorageMonitor {

    IItemList<IAEItemStack> refreshExternalChanges(BaseActionSource source, boolean force);
}
