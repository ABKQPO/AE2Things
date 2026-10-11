package com.asdflj.ae2thing.crossmod.waila;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.client.gui.GuiWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.client.gui.widget.DualPatternSlot;
import com.asdflj.ae2thing.proxy.ClientProxy;

import appeng.container.slot.SlotFake;
import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.AcceptsFollowingTooltipLineHandler;
import codechicken.nei.recipe.RecipeHandlerRef;

public class PatternPermutationToolTip extends mcp.mobius.waila.handlers.nei.TooltipHandlerWaila {

    protected AcceptsFollowingTooltipLineHandler acceptsFollowingTooltipLineHandler;

    @Override
    public List<String> handleItemTooltip(GuiContainer gui, ItemStack itemstack, int x, int y,
        List<String> currenttip) {
        if (!NEIClientConfig.showCycledIngredientsTooltip() || ClientProxy.getOverlayButton() == null) {
            return currenttip;
        }

        ItemStack slotItem = null;
        if (gui instanceof GuiWirelessDualInterfaceTerminal terminal
            && terminal.getVirtualMESlotUnderMouse() instanceof DualPatternSlot slot
            && slot.getAEStack() != null) {
            slotItem = slot.getAEStack()
                .getItemStackForNEI();
        } else if (gui.theSlot instanceof SlotFake slot && slot.getHasStack()) {
            slotItem = slot.getStack();
        }
        if (slotItem == null) {
            return currenttip;
        }

        RecipeHandlerRef ref = ClientProxy.getOverlayButton().handlerRef;
        for (PositionedStack positionedStack : ref.handler.getIngredientStacks(ref.recipeIndex)) {
            if (positionedStack.items.length > 1 && positionedStack.containsWithNBT(slotItem)) {
                if (this.acceptsFollowingTooltipLineHandler == null
                    || this.acceptsFollowingTooltipLineHandler.tooltipGUID != positionedStack) {
                    this.acceptsFollowingTooltipLineHandler = AcceptsFollowingTooltipLineHandler
                        .of(positionedStack, positionedStack.getFilteredPermutations(), positionedStack.item);
                }

                if (this.acceptsFollowingTooltipLineHandler != null) {
                    this.acceptsFollowingTooltipLineHandler.setActiveStack(slotItem);
                    currenttip
                        .add(GuiDraw.TOOLTIP_HANDLER + GuiDraw.getTipLineId(this.acceptsFollowingTooltipLineHandler));
                }
                break;
            }
        }

        return currenttip;
    }

}
