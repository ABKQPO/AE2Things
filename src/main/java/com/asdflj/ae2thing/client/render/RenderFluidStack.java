package com.asdflj.ae2thing.client.render;

import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.client.gui.IGuiDrawSlot;
import com.glodblock.github.common.item.ItemFluidDrop;
import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStack;
import codechicken.nei.recipe.StackInfo;

public class RenderFluidStack implements ISlotRender {

    @Override
    public Predicate<Slot> get() {
        return slot -> {
            ItemStack stack = slot.getStack();
            if (stack == null || stack.getItem() == null) return false;
            return stack.getItem() instanceof ItemFluidDrop || stack.getItem() instanceof ItemFluidPacket
                || StackInfo.getFluid(stack) != null;
        };
    }

    @Override
    public boolean drawSlot(Slot slot, IAEStack<?> stack, IGuiDrawSlot draw, boolean display) {
        if (!(stack instanceof IAEFluidStack fluid) || fluid.getFluidStack() == null) return true;

        final Minecraft mc = Minecraft.getMinecraft();
        fluid.drawInGui(mc, slot.xDisplayPosition, slot.yDisplayPosition);
        fluid.drawOverlayInGui(mc, slot.xDisplayPosition, slot.yDisplayPosition, true, true, false, false);
        return false;
    }
}
