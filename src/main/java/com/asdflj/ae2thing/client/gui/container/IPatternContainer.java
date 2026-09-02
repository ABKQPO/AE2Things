package com.asdflj.ae2thing.client.gui.container;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.inventory.IPatternTerminal;
import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.container.slot.SlotFake;

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

    default boolean canDouble(SlotFake[] slots, int mult) {
        if (mult == 0) return false;
        for (Slot s : slots) {
            ItemStack st = s.getStack();
            if (st != null) {
                long result;
                if (mult < 0) {
                    result = (long) getStackSize(st) / Math.abs(mult);
                } else {
                    result = (long) getStackSize(st) * mult;
                }
                if (result > Integer.MAX_VALUE || result <= 0) {
                    return false;
                }
            }
        }
        return true;
    }

    default void doubleStacksInternal(SlotFake[] slots, int mult) {
        if (mult == 0) return;
        for (final SlotFake s : slots) {
            if (!s.isEnabled()) continue;
            ItemStack st = s.getStack();
            if (st != null) {
                if (mult < 0) {
                    if (st.getItem() instanceof ItemFluidPacket) {
                        ItemFluidPacket.setFluidAmount(st, ItemFluidPacket.getFluidAmount(st) / Math.abs(mult));
                    } else {
                        st.stackSize /= Math.abs(mult);
                    }
                } else {
                    if (st.getItem() instanceof ItemFluidPacket) {
                        ItemFluidPacket.setFluidAmount(st, ItemFluidPacket.getFluidAmount(st) * mult);
                    } else {
                        st.stackSize *= mult;
                    }
                }
            }
        }
    }

    Slot getPatternOutputSlot();

    default Slot getPatternInputSlot() {
        return null;
    }
}
