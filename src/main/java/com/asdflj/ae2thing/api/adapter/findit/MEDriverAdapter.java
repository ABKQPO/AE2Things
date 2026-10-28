package com.asdflj.ae2thing.api.adapter.findit;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.util.StorageProvider;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.DimensionalCoord;
import appeng.tile.storage.TileDrive;

public class MEDriverAdapter implements IFindItAdapter {

    @Override
    public Class<? extends IGridHost> getCls() {
        return TileDrive.class;
    }

    @Override
    public List<StorageProvider> getStorageProver(IGrid grid, IGridNode node, IAEStack<?> request) {
        List<StorageProvider> list = new ArrayList<>();
        if (node.getMachine() instanceof TileDrive drive) {
            for (int i = 0; i < drive.getCellCount(); i++) {
                ItemStack is = drive.getStackInSlot(i);
                if (this.findStack(this.getCellInventory(is, request.getStackType()), request)) {
                    list.add(new StorageProvider(new DimensionalCoord(drive), i));
                }
            }
        }
        return list;
    }

}
