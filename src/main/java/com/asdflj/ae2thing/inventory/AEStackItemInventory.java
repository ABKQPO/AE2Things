package com.asdflj.ae2thing.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.tile.inventory.IAEStackInventory;
import appeng.tile.inventory.IIAEStackInventory;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.item.ItemFluidDisplay;

public class AEStackItemInventory implements IIAEStackInventory {

    private static final int FORMAT_AE_STACK = 1;
    private static final String FORMAT_TAG = "#ae2thingFormat";

    private final ItemStack owner;
    private final String inventoryName;
    private final EntityPlayer player;
    private final int slot;
    private final StorageName storageName;
    private final IAEStackInventory aeInventory;

    public AEStackItemInventory(ItemStack is, String name, int size, EntityPlayer player, int slot,
        StorageName storageName) {
        this.owner = is;
        this.inventoryName = name;
        this.player = player;
        this.slot = slot;
        this.storageName = storageName;
        this.aeInventory = new IAEStackInventory(this, size, storageName);
        this.loadFromNBT();
    }

    public IAEStackInventory getAEInventory() {
        return this.aeInventory;
    }

    @Override
    public IAEStackInventory getAEInventoryByName(StorageName name) {
        return this.storageName == name ? this.aeInventory : null;
    }

    public int getSizeInventory() {
        return this.aeInventory.getSizeInventory();
    }

    public IAEStack<?> getAEStackInSlot(int slot) {
        return slot >= 0 && slot < this.getSizeInventory() ? this.aeInventory.getAEStackInSlot(slot) : null;
    }

    public void putAEStackInSlot(int slot, IAEStack<?> stack) {
        if (slot < 0 || slot >= this.getSizeInventory()) return;
        this.aeInventory.putAEStackInSlot(slot, stack);
    }

    @Override
    public void saveAEStackInv() {
        if (!Platform.isServer()) return;
        this.aeInventory.writeToNBT(this.owner, this.inventoryName);
        final NBTTagCompound data = this.owner.getTagCompound();
        if (data != null && data.hasKey(this.inventoryName)) {
            data.getCompoundTag(this.inventoryName)
                .setInteger(FORMAT_TAG, FORMAT_AE_STACK);
        }
        if (this.slot != -1) {
            this.player.inventory.setInventorySlotContents(this.slot, this.owner);
        } else {
            this.player.inventory.setItemStack(this.owner);
        }
    }

    private void loadFromNBT() {
        final NBTTagCompound data = this.owner.getTagCompound();
        final NBTTagCompound stored = data == null ? null : data.getCompoundTag(this.inventoryName);
        if (stored != null && stored.getInteger(FORMAT_TAG) == FORMAT_AE_STACK) {
            this.aeInventory.readFromNBT(data, this.inventoryName);
            return;
        }
        if (stored == null) return;
        for (int i = 0; i < this.getSizeInventory(); i++) {
            final NBTTagCompound slotTag = stored.getCompoundTag("#" + i);
            if (slotTag.hasNoTags()) continue;
            final IAEStack<?> stack = toAEStack(ItemStack.loadItemStackFromNBT(slotTag));
            if (stack != null) {
                this.aeInventory.putAEStackInSlot(i, stack);
            }
        }
    }

    public static IAEStack<?> toAEStack(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;

        if (stack.getItem() instanceof ItemFluidDisplay display) {
            final FluidStack fluid = display.getFluid(stack);
            if (fluid != null) {
                final IAEFluidStack out = AEFluidStack.create(fluid);
                out.setStackSize(Math.max(display.getAmountLong(stack), 0));
                return out;
            }
        }
        if (stack.getItem() instanceof ItemFluidPacket) {
            final IAEFluidStack fluid = ItemFluidPacket.getFluidAEStack(stack);
            if (fluid != null) return fluid;
        }
        if (stack.getItem() instanceof ItemFluidDrop) {
            final FluidStack fluid = ItemFluidDrop.getFluidStack(stack);
            if (fluid != null) return AEFluidStack.create(fluid);
        }
        return AEItemStack.create(stack);
    }

    public static ItemStack toItemStack(IAEStack<?> stack) {
        if (stack == null) return null;
        if (stack instanceof IAEFluidStack fluid && fluid.getFluidStack() != null) {
            if (Platform.isClient()) {
                final ItemStack display = ItemFluidDisplay.createStack(
                    fluid.getFluidStack()
                        .getFluid(),
                    stack.getStackSize());
                if (display != null) return display;
            }
            return ItemFluidPacket.newStack(fluid);
        }
        if (stack instanceof IAEItemStack item) {
            final int size = (int) Math.min(Integer.MAX_VALUE, stack.getStackSize());
            final ItemStack is = item.getItemStack()
                .copy();
            is.stackSize = Math.max(size, 1);
            return is;
        }
        return stack.getItemStackForNEI();
    }
}
