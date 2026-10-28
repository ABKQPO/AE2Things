package com.asdflj.ae2thing.api.adapter.findit;

import java.util.List;

import javax.annotation.Nullable;

import com.asdflj.ae2thing.util.StorageProvider;
import com.google.common.collect.ImmutableList;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.storage.ICellContainer;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.DimensionalCoord;
import appeng.parts.AEBasePart;

public class PartStorageBusAdapter implements IFindItAdapter {

    private final Class<? extends IGridHost> hostClass;

    public PartStorageBusAdapter(Class<? extends IGridHost> hostClass) {
        this.hostClass = hostClass;
    }

    @Override
    public Class<? extends IGridHost> getCls() {
        return this.hostClass;
    }

    @Override
    @SuppressWarnings("rawtypes")
    @Nullable
    public List<StorageProvider> getStorageProver(IGrid grid, IGridNode node, IAEStack<?> request) {
        if (!(node.getMachine() instanceof AEBasePart part) || !(part instanceof ICellContainer container)) {
            return null;
        }
        for (IMEInventoryHandler handler : container.getCellArray(request.getStackType())) {
            if (this.findStack(handler, request)) {
                return ImmutableList.of(new StorageProvider(new DimensionalCoord(part.getTile()), part.getSide()));
            }
        }
        return null;
    }
}
