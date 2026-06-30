package com.asdflj.ae2thing.common.storage;

import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.data.IAEFluidStack;

public interface ITFluidCellInventoryHandler extends ICellInventoryHandler<IAEFluidStack> {

    @Override
    ITFluidCellInventory getCellInv();

    Iterable<IAEFluidStack> getPartitionInv();
}
