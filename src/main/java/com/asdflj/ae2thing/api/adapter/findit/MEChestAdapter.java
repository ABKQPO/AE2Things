package com.asdflj.ae2thing.api.adapter.findit;

import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.asdflj.ae2thing.util.StorageProvider;
import com.google.common.collect.ImmutableList;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.DimensionalCoord;
import appeng.tile.storage.TileChest;

public class MEChestAdapter implements IFindItAdapter {

    @Override
    public Class<? extends IGridHost> getCls() {
        return TileChest.class;
    }

    @Override
    @Nullable
    public List<StorageProvider> getStorageProver(IGrid grid, IGridNode node, IAEStack<?> request) {
        if (!(node.getMachine() instanceof TileChest chest)) return null;
        ItemStack cell = chest.getInternalInventory()
            .getStackInSlot(1);
        if (this.findStack(this.getCellInventory(cell, request.getStackType()), request)) {
            return ImmutableList.of(new StorageProvider(new DimensionalCoord(chest), 1));
        }
        return null;
    }
}
