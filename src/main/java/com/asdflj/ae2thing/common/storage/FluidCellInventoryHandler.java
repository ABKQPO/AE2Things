package com.asdflj.ae2thing.common.storage;

import static appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE;

import com.asdflj.ae2thing.util.Util;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.me.storage.CellInventoryHandler;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.item.AEFluidStack;
import appeng.util.prioitylist.PrecisePriorityList;

public class FluidCellInventoryHandler extends CellInventoryHandler<IAEFluidStack>
    implements ITFluidCellInventoryHandler {

    public FluidCellInventoryHandler(final IMEInventory<IAEFluidStack> c) {
        super(c, FLUID_STACK_TYPE);
    }

    @Override
    protected void setOreFilteredList(String filter) {
        // fluids do not support ore dictionary filtering
    }

    @Override
    protected void setPriorityList(boolean hasFuzzy, IAEStackInventory config, FuzzyMode fzMode) {
        final IItemList<IAEFluidStack> priorityList = AEApi.instance()
            .storage()
            .createFluidList();
        for (int x = 0; x < config.getSizeInventory(); x++) {
            final IAEStack<?> aes = config.getAEStackInSlot(x);
            final IAEFluidStack fluid;
            if (aes instanceof IAEFluidStack afs) {
                fluid = afs;
            } else if (aes instanceof IAEItemStack ais) {
                fluid = AEFluidStack.create(Util.getFluidFromItem(ais.getItemStack()));
            } else {
                fluid = null;
            }
            if (fluid != null) {
                fluid.setStackSize(1);
                priorityList.add(fluid);
                if (aes instanceof IAEItemStack) {
                    config.putAEStackInSlot(x, fluid);
                    config.markDirty();
                }
            }
        }
        if (!priorityList.isEmpty()) {
            this.setPartitionList(new PrecisePriorityList<>(priorityList));
        }
    }

    @Override
    public ITFluidCellInventory getCellInv() {
        return super.getCellInv() instanceof ITFluidCellInventory ti ? ti : null;
    }

    @Override
    public Iterable<IAEFluidStack> getPartitionInv() {
        return this.getPartitionList()
            .getItems();
    }

    @Override
    public StorageChannel getStorageChannel() {
        return StorageChannel.FLUIDS;
    }

    @Override
    public int getStatusForCell() {
        int val = this.getCellInv()
            .getStatusForCell();

        if (val == 1 && this.isPreformatted()) {
            val = 2;
        }

        return val;
    }
}
