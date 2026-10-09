package com.asdflj.ae2thing.client.gui.container;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.inventory.IPatternTerminal;
import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.storage.data.IAEStack;
import appeng.container.slot.SlotFake;
import appeng.tile.inventory.IAEStackInventory;

public interface IPatternContainer {

    boolean isPatternTerminal();

    boolean hasRefillerUpgrade();

    void refillBlankPatterns(Slot slot);

    void encode();

    void encodeAndMoveToInventory();

    void encodeAllItemAndMoveToInventory();

    IPatternTerminal getPatternTerminal();

    void clear();

    void doubleStacks(int value);

    default int getStackSize(ItemStack stack) {
        if (stack.getItem() instanceof ItemFluidPacket) {
            return (int) ItemFluidPacket.getFluidAmount(stack);
        } else {
            return stack.stackSize;
        }
    }

    default long getAEStackSize(Slot slot) {
        ItemStack stack = slot.getStack();
        return stack == null ? 0 : getStackSize(stack);
    }

    default boolean canDouble(SlotFake[] slots, int mult) {
        if (mult == 0) return false;
        for (Slot s : slots) {
            long size = getAEStackSize(s);
            if (size <= 0) continue;
            double result = mult < 0 ? (double) size / Math.abs(mult) : (double) size * mult;
            if (result > Long.MAX_VALUE || result <= 0) {
                return false;
            }
        }
        return true;
    }

    default void doubleStacksInternal(SlotFake[] slots, int mult) {
        if (mult == 0) return;
        for (final SlotFake s : slots) {
            if (!s.isEnabled()) continue;
            long size = getAEStackSize(s);
            if (size <= 0) continue;
            long result = mult < 0 ? size / Math.abs(mult) : size * mult;

            ItemStack st = s.getStack();
            if (st == null) continue;
            if (st.getItem() instanceof ItemFluidPacket) {
                ItemFluidPacket.setFluidAmount(st, result);
            } else {
                st.stackSize = (int) result;
            }
        }
    }

    Slot getPatternOutputSlot();

    default boolean canDouble(IAEStackInventory inv, int mult) {
        if (mult == 0 || inv == null) return false;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            final IAEStack<?> stack = inv.getAEStackInSlot(i);
            if (stack == null) continue;
            final long size = stack.getStackSize();
            if (size <= 0) continue;
            final double result = mult < 0 ? (double) size / Math.abs(mult) : (double) size * mult;
            if (result > Long.MAX_VALUE || result <= 0) {
                return false;
            }
        }
        return true;
    }

    default void doubleStacksInternal(IAEStackInventory inv, int mult) {
        if (mult == 0 || inv == null) return;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            final IAEStack<?> stack = inv.getAEStackInSlot(i);
            if (stack == null) continue;
            final long size = stack.getStackSize();
            if (size <= 0) continue;
            final long result = mult < 0 ? size / Math.abs(mult) : size * mult;
            final IAEStack<?> copy = stack.copy();
            copy.setStackSize(result);
            inv.putAEStackInSlot(i, copy);
        }
    }

    default Slot getPatternInputSlot() {
        return null;
    }
}
