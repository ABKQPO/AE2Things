package com.asdflj.ae2thing.common.storage;

import appeng.api.networking.security.BaseActionSource;

public interface RefreshableStorageMonitor {

    void refreshExternalChanges(BaseActionSource source);
}
