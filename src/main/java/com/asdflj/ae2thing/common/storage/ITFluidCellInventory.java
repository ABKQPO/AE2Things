package com.asdflj.ae2thing.common.storage;

import java.util.List;

import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.common.storage.infinityCell.BaseInventory;

import appeng.api.storage.ICellInventory;
import appeng.api.storage.data.IAEFluidStack;

public interface ITFluidCellInventory extends ICellInventory<IAEFluidStack>, BaseInventory {

    /**
     * @return idle cost for this Storage Cell, kept for compatibility with the cell handler lookup.
     */
    double getIdleDrain(ItemStack is);

    /**
     * @return a snapshot of the fluids currently held by this cell.
     */
    List<IAEFluidStack> getContents();

    @Override
    default long getRemainingItemsCountDist(IAEFluidStack l) {
        return 0;
    }
}
