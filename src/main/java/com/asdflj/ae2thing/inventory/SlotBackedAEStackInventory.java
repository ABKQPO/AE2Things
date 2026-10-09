package com.asdflj.ae2thing.inventory;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStack;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.item.AEItemStack;

public class SlotBackedAEStackInventory extends IAEStackInventory {

    private final IInventory inv;

    public SlotBackedAEStackInventory(IInventory inv, StorageName name) {
        super(null, inv == null ? 0 : inv.getSizeInventory(), name);
        this.inv = inv;
    }

    @Override
    public int getSizeInventory() {
        return this.inv == null ? 0 : this.inv.getSizeInventory();
    }

    @Override
    public boolean isEmpty() {
        if (this.inv == null) return true;
        for (int i = 0; i < this.inv.getSizeInventory(); i++) {
            if (this.getAEStackInSlot(i) != null) return false;
        }
        return true;
    }

    @Override
    public IAEStack<?> getAEStackInSlot(int n) {
        if (this.inv == null || n < 0 || n >= this.inv.getSizeInventory()) return null;
        final ItemStack stack = this.inv.getStackInSlot(n);
        if (stack == null) return null;
        final IAEFluidStack fluid = ItemFluidPacket.getFluidAEStack(stack);
        if (fluid != null) return fluid;
        return AEItemStack.create(stack);
    }

    @Override
    public void putAEStackInSlot(int n, IAEStack<?> aes) {}
}
