package com.asdflj.ae2thing.api.adapter.findit;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.asdflj.ae2thing.util.StorageProvider;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.util.IterationCounter;

public interface IFindItAdapter {

    Class<? extends IGridHost> getCls();

    default List<StorageProvider> getStorageProviders(IGrid grid, IAEStack<?> request) {
        if (request == null) return new ArrayList<>();
        List<StorageProvider> list = new ArrayList<>();
        for (IGridNode node : grid.getMachines(getCls())) {
            List<StorageProvider> results = getStorageProver(grid, node, request);
            if (results != null && !results.isEmpty()) {
                list.addAll(results);
            }
        }
        return list;
    }

    @Nullable
    List<StorageProvider> getStorageProver(IGrid grid, IGridNode node, IAEStack<?> request);

    @SuppressWarnings({ "rawtypes", "unchecked" })
    default boolean findStack(IMEInventory inv, IAEStack<?> request) {
        if (inv == null || request == null) return false;
        if (inv.getStackType() != request.getStackType()) return false;
        return inv.getAvailableItem(request, IterationCounter.fetchNewId()) != null;
    }

    @SuppressWarnings("rawtypes")
    @Nullable
    default IMEInventoryHandler getCellInventory(ItemStack cell, IAEStackType<?> type) {
        if (cell == null || cell.getItem() == null) return null;
        return AEApi.instance()
            .registries()
            .cell()
            .getCellInventory(cell, null, type);
    }
}
