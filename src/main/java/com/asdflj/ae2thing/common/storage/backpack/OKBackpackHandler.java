package com.asdflj.ae2thing.common.storage.backpack;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import ruiseki.okbackpack.common.block.BackpackWrapper;
import ruiseki.okbackpack.common.block.BlockBackpack.ItemBackpack;
import ruiseki.okbackpack.common.helpers.BackpackEntityHelpers;
import ruiseki.okbackpack.common.helpers.BackpackEntityHelpers.BackpackContext;
import ruiseki.okbackpack.common.helpers.BackpackEntityHelpers.SearchOrder;

public class OKBackpackHandler extends BaseBackpackHandler {

    private final BackpackContext context;

    public OKBackpackHandler(BackpackContext context) {
        super(new OKBackpackInventory(context));
        this.context = context;
    }

    public static void addPlayerBackpacks(EntityPlayer player, List<BaseBackpackHandler> output) {
        BackpackEntityHelpers.visitPlayerBackpacks(player, SearchOrder.PLAYER_THEN_BAUBLES, context -> {
            output.add(new OKBackpackHandler(context));
            return false;
        });
    }

    public static Class<? extends Item> getBackpackItemClass() {
        return ItemBackpack.class;
    }

    @Override
    public void markDirty() {
        super.markDirty();
        BackpackEntityHelpers.persistBackpack(this.context);
    }

    @Override
    public ItemStack injectItem(ItemStack stack) {
        ItemStack remaining = this.context.getWrapper()
            .insertItem(stack.copy(), false);
        BackpackEntityHelpers.persistBackpack(this.context);
        return remaining == null ? null : remaining;
    }

    public static class OKBackpackInventory implements IInventory {

        private final BackpackContext context;
        private final BackpackWrapper wrapper;

        public OKBackpackInventory(BackpackContext context) {
            this.context = context;
            this.wrapper = context.getWrapper();
        }

        @Override
        public int getSizeInventory() {
            return this.wrapper.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            ItemStack stack = this.wrapper.getStackInSlot(slot);
            if (stack == null || this.wrapper.canExtract(slot, stack)) {
                return stack;
            }
            return null;
        }

        @Override
        public ItemStack decrStackSize(int slot, int amount) {
            ItemStack extracted = this.wrapper.extractItem(slot, amount, false);
            if (extracted != null) {
                this.markDirty();
            }
            return extracted;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int slot) {
            ItemStack stack = this.getStackInSlot(slot);
            if (stack != null) {
                this.wrapper.extractItem(slot, stack.stackSize, false);
                this.markDirty();
            }
            return stack;
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            this.wrapper.setStackInSlot(slot, stack);
            this.markDirty();
        }

        @Override
        public String getInventoryName() {
            return this.wrapper.getInventoryName();
        }

        @Override
        public boolean hasCustomInventoryName() {
            return this.wrapper.hasCustomInventoryName();
        }

        @Override
        public int getInventoryStackLimit() {
            return Integer.MAX_VALUE;
        }

        @Override
        public void markDirty() {
            this.wrapper.markDirty();
            BackpackEntityHelpers.persistBackpack(this.context);
        }

        @Override
        public boolean isUseableByPlayer(EntityPlayer player) {
            return this.wrapper.canPlayerAccess(player.getUniqueID());
        }

        @Override
        public void openInventory() {}

        @Override
        public void closeInventory() {
            BackpackEntityHelpers.persistBackpack(this.context);
        }

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            if (stack == null) {
                return true;
            }
            ItemStack single = stack.copy();
            single.stackSize = 1;
            return this.wrapper.canInsert(slot, single) && this.wrapper.insertItem(slot, single, true) == null;
        }
    }
}
