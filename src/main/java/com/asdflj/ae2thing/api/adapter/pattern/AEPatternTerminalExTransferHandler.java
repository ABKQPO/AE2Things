package com.asdflj.ae2thing.api.adapter.pattern;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;

import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.nei.NEIUtils;
import com.asdflj.ae2thing.nei.object.OrderStack;
import com.asdflj.ae2thing.network.CPacketTransferRecipe;
import com.asdflj.ae2thing.util.GTNLUtil;

import appeng.api.parts.IPatternTerminalEx;
import appeng.container.implementations.ContainerPatternTermEx;

public class AEPatternTerminalExTransferHandler implements IRecipeHandler {

    @Override
    public void transferPack(Container container, List<OrderStack<?>> inputs, List<OrderStack<?>> outputs,
        String identifier, IPatternTerminalAdapter adapter, CPacketTransferRecipe message) {
        ContainerPatternTermEx c = (ContainerPatternTermEx) container;
        IInventory inputSlot = adapter.getInventoryByName(c, Constants.CRAFTING);
        IInventory outputSlot = adapter.getInventoryByName(c, Constants.OUTPUT);
        inputs = NEIUtils.clearNull(inputs);
        outputs = NEIUtils.clearNull(outputs);

        final int pages = GTNLUtil.isPatternCapacityEnabled() ? GTNLUtil.patternPages() : 0;
        if (pages > 0) {
            transferPage(c, inputSlot, outputSlot, adapter, inputs, outputs, pages);
            return;
        }

        for (int i = 0; i < inputSlot.getSizeInventory(); i++) {
            inputSlot.setInventorySlotContents(i, null);
        }
        for (int i = 0; i < outputSlot.getSizeInventory(); i++) {
            outputSlot.setInventorySlotContents(i, null);
        }
        adapter.transferPack(inputs, inputSlot);
        adapter.transferPack(outputs, outputSlot);
        c.onCraftMatrixChanged(inputSlot);
        c.onCraftMatrixChanged(outputSlot);
        c.getPatternTerminal()
            .saveChanges();
    }

    private void transferPage(ContainerPatternTermEx c, IInventory inputSlot, IInventory outputSlot,
        IPatternTerminalAdapter adapter, List<OrderStack<?>> inputs, List<OrderStack<?>> outputs, int pages) {
        int page = 0;
        boolean inverted = false;
        if (c.getPatternTerminal() instanceof IPatternTerminalEx ex) {
            page = Math.clamp(ex.getActivePage(), 0, pages - 1);
            inverted = ex.isInverted();
        }
        final int inputPageSize = inverted ? GTNLUtil.smallSideSlots() : GTNLUtil.largeSideSlots();
        final int outputPageSize = inverted ? GTNLUtil.largeSideSlots() : GTNLUtil.smallSideSlots();
        final int inputOffset = page * inputPageSize;
        final int outputOffset = page * outputPageSize;

        clearRange(inputSlot, inputOffset, inputPageSize);
        clearRange(outputSlot, outputOffset, outputPageSize);
        adapter.transferPack(rebase(inputs, inputOffset, inputPageSize), inputSlot);
        adapter.transferPack(rebase(outputs, outputOffset, outputPageSize), outputSlot);
        c.onCraftMatrixChanged(inputSlot);
        c.onCraftMatrixChanged(outputSlot);
        c.getPatternTerminal()
            .saveChanges();
    }

    private static void clearRange(IInventory inv, int offset, int size) {
        final int end = Math.min(inv.getSizeInventory(), offset + size);
        for (int i = Math.max(0, offset); i < end; i++) {
            inv.setInventorySlotContents(i, null);
        }
    }

    private static List<OrderStack<?>> rebase(List<OrderStack<?>> stacks, int offset, int size) {
        final List<OrderStack<?>> result = new ArrayList<>(stacks.size());
        for (OrderStack<?> stack : stacks) {
            if (stack == null) continue;
            final int local = stack.getIndex();
            if (local < 0 || local >= size) continue;
            final OrderStack<?> shifted = new OrderStack<>(stack.getStack(), offset + local);
            result.add(shifted);
        }
        return result;
    }
}
