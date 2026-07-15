package com.asdflj.ae2thing.api.adapter.findit;

import java.util.ArrayList;
import java.util.List;

import com.asdflj.ae2thing.common.item.ItemPhial;
import com.asdflj.ae2thing.util.StorageProvider;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.DimensionalCoord;
import appeng.util.IterationCounter;
import thaumicenergistics.common.parts.PartEssentiaStorageBus;
import thaumicenergistics.common.storage.AEEssentiaStack;
import thaumicenergistics.common.storage.AEEssentiaStackType;

public class EssentiaStorageBusAdapter implements IFindItAdapter {

    @Override
    public Class<? extends IGridHost> getCls() {
        return PartEssentiaStorageBus.class;
    }

    @Override
    public boolean supportFluid() {
        return true;
    }

    @Override
    public List<StorageProvider> getStorageProver(IGrid grid, IGridNode node, IAEItemStack item, boolean isFluid) {
        List<StorageProvider> list = new ArrayList<>();
        if (node.getMachine() instanceof PartEssentiaStorageBus bus) {
            AEEssentiaStack request = ItemPhial.getAeEssentiaStack(item);
            if (request == null) {
                request = AEEssentiaStackType.ESSENTIA_STACK_TYPE.getStackFromContainerItem(item.getItemStack());
            }
            if (request == null) {
                request = AEEssentiaStackType.ESSENTIA_STACK_TYPE.convertStackFromItem(item.getItemStack());
            }
            if (request == null) return list;

            List<IMEInventoryHandler> handlers = bus.getCellArray(AEEssentiaStackType.ESSENTIA_STACK_TYPE);
            for (IMEInventoryHandler handler : handlers) {
                if (handler.getStackType() == AEEssentiaStackType.ESSENTIA_STACK_TYPE
                    && handler.getAvailableItem(request, IterationCounter.fetchNewId()) != null) {
                    list.add(
                        new StorageProvider(
                            new DimensionalCoord(
                                bus.getHost()
                                    .getTile()),
                            bus.getSide()));
                    break;
                }
            }

        }
        return list;
    }
}
