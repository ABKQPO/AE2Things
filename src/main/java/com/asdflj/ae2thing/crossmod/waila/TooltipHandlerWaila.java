package com.asdflj.ae2thing.crossmod.waila;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

public class TooltipHandlerWaila extends mcp.mobius.waila.handlers.nei.TooltipHandlerWaila {

    @Override
    public List<String> handleItemDisplayName(GuiContainer arg0, ItemStack itemstack, List<String> currenttip) {
        return currenttip;
    }

    @Override
    public List<String> handleItemTooltip(GuiContainer arg0, ItemStack itemstack, int arg2, int arg3,
        List<String> currentToolTip) {
        return currentToolTip;
    }

    @Override
    public List<String> handleTooltip(GuiContainer arg0, int arg1, int arg2, List<String> currenttip) {
        return currenttip;
    }
}
