package com.asdflj.ae2thing.common.storage.backpack;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidTank;

import appeng.util.Platform;

public abstract class BaseBackpackHandler implements IInventory {

    protected final IInventory inv;

    public BaseBackpackHandler(IInventory inv) {
        this.inv = inv;
    }

    @Override
    public int getSizeInventory() {
        return inv.getSizeInventory();
    }

    @Override
    public ItemStack getStackInSlot(int slotIn) {
        return this.inv.getStackInSlot(slotIn);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return this.inv.decrStackSize(index, count);
    }

    public boolean hasFluidTank() {
        return false;
    }

    public List<FluidTank> getFluidTanks() {
        return Collections.emptyList();
    }

    public void markFluidAsDirty() {

    }

    public ItemStack injectItem(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < this.getSizeInventory(); i++) {
            ItemStack slotItem = this.getStackInSlot(i);
            if (slotItem == null || !Platform.isSameItemPrecise(slotItem, remaining)
                || !this.isItemValidForSlot(i, remaining)) {
                continue;
            }
            int moved = Math.min(this.getInsertableAmount(i, slotItem), remaining.stackSize);
            if (moved <= 0) {
                continue;
            }
            ItemStack updated = slotItem.copy();
            updated.stackSize += moved;
            this.setInventorySlotContents(i, updated);
            remaining.stackSize -= moved;
            if (remaining.stackSize <= 0) {
                return remaining;
            }
        }
        for (int i = 0; i < this.getSizeInventory(); i++) {
            if (this.getStackInSlot(i) != null || !this.isItemValidForSlot(i, remaining)) {
                continue;
            }
            ItemStack added = remaining.copy();
            added.stackSize = Math.min(this.getSlotStackLimit(i, added), remaining.stackSize);
            if (added.stackSize <= 0) {
                continue;
            }
            this.setInventorySlotContents(i, added);
            remaining.stackSize -= added.stackSize;
            if (remaining.stackSize <= 0) {
                return remaining;
            }
        }
        return remaining;
    }

    protected int getSlotStackLimit(int slot, ItemStack stack) {
        int itemLimit = stack.getMaxStackSize();
        if (itemLimit <= 1) {
            return itemLimit;
        }
        int inventoryLimit = this.getInventoryStackLimit();
        if (inventoryLimit > itemLimit) {
            return inventoryLimit;
        }
        return Math.min(itemLimit, inventoryLimit);
    }

    private int getInsertableAmount(int slot, ItemStack stack) {
        return this.getSlotStackLimit(slot, stack) - stack.stackSize;
    }

    public ItemStack extractItem(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < this.getSizeInventory(); i++) {
            ItemStack slotStack = this.getStackInSlot(i);
            if (slotStack == null || !Platform.isSameItemPrecise(slotStack, remaining)) {
                continue;
            }
            int size = slotStack.stackSize;
            if (size > remaining.stackSize) {
                slotStack.splitStack(remaining.stackSize);
                this.setInventorySlotContents(i, slotStack.copy());
                return stack;
            }
            this.setInventorySlotContents(i, null);
            remaining.stackSize -= size;
            if (remaining.stackSize <= 0) {
                return stack;
            }
        }
        remaining.stackSize = stack.stackSize - remaining.stackSize;
        return remaining;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        return this.inv.getStackInSlotOnClosing(index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        this.inv.setInventorySlotContents(index, stack);
    }

    @Override
    public String getInventoryName() {
        return this.inv.getInventoryName();
    }

    @Override
    public boolean hasCustomInventoryName() {
        return this.inv.hasCustomInventoryName();
    }

    @Override
    public int getInventoryStackLimit() {
        return this.inv.getInventoryStackLimit();
    }

    @Override
    public void markDirty() {
        this.inv.markDirty();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return this.inv.isUseableByPlayer(player);
    }

    @Override
    public void openInventory() {
        this.inv.openInventory();
    }

    @Override
    public void closeInventory() {
        this.inv.closeInventory();
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return this.inv.isItemValidForSlot(index, stack);
    }
}
