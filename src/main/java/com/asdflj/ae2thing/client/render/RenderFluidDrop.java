package com.asdflj.ae2thing.client.render;

import static appeng.client.gui.AEBaseGui.aeRenderItem;

import java.util.function.Predicate;

import net.minecraft.inventory.Slot;
import net.minecraftforge.fluids.FluidStack;

import com.asdflj.ae2thing.client.gui.IGuiDrawSlot;
import com.glodblock.github.common.item.ItemFluidDrop;

import appeng.api.storage.data.IAEItemStack;

public class RenderFluidDrop implements ISlotRender {

    @Override
    public Predicate<Slot> get() {
        return slot -> slot.getStack()
            .getItem() instanceof ItemFluidDrop;
    }

    @Override
    public boolean drawSlot(Slot slot, IAEItemStack stack, IGuiDrawSlot draw, boolean display) {
        FluidStack fluidStack = ItemFluidDrop.getFluidStack(slot.getStack());
        if (fluidStack == null || fluidStack.getFluid() == null) return true;
        draw.drawWidget(slot.xDisplayPosition, slot.yDisplayPosition, fluidStack.getFluid());
        aeRenderItem.setAeStack(stack);
        draw.renderStackSize(display, stack, slot);
        return false;
    }
}
