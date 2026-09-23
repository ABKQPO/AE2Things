package com.asdflj.ae2thing.loader;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.api.AE2ThingAPI;
import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.api.adapter.pattern.FCPatternTerminal;
import com.asdflj.ae2thing.api.adapter.pattern.IRecipeHandler;
import com.asdflj.ae2thing.api.adapter.pattern.THDualInterfacePatternTerminal;
import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.inventory.IPatternTerminal;
import com.asdflj.ae2thing.nei.NEIUtils;
import com.asdflj.ae2thing.nei.object.OrderStack;

import appeng.api.storage.data.IAEStack;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.item.AEItemStack;

public class BRLoader implements Runnable {

    @Override
    public void run() {
        IRecipeHandler handler = (container, inputs, outputs, identifier, adapter, message) -> {
            if (container instanceof ContainerPatternTerm c) {
                c.setCraftingMode(false);
                IInventory inputSlot = adapter.getInventoryByName(c, adapter.getCraftingInvName());
                IInventory outputSlot = adapter.getInventoryByName(c, adapter.getOutputInvName());
                if (inputSlot == null || outputSlot == null) {
                    final IAEStackInventory inputsInv = c.inputsSync.get();
                    final IAEStackInventory outputsInv = c.outputsSync.get();
                    for (int i = 0; i < inputsInv.getSizeInventory(); i++) {
                        inputsInv.putAEStackInSlot(i, null);
                    }
                    for (int i = 0; i < outputsInv.getSizeInventory(); i++) {
                        outputsInv.putAEStackInSlot(i, null);
                    }
                    for (OrderStack<?> stack : NEIUtils.clearNull(inputs)) {
                        if (stack.getStack() instanceof ItemStack item) {
                            putAEStack(inputsInv, stack.getIndex(), AEItemStack.create(item.copy()));
                        }
                    }
                    for (OrderStack<?> stack : NEIUtils.clearNull(outputs)) {
                        if (stack.getStack() instanceof ItemStack item) {
                            putAEStack(outputsInv, stack.getIndex(), AEItemStack.create(item.copy()));
                        }
                    }
                    // let the pattern terminal sync handlers push the new slots to the client
                    c.inputsSync.markDirty();
                    c.outputsSync.markDirty();
                    c.getPatternTerminal()
                        .saveChanges();
                    return;
                }
                for (int i = 0; i < inputSlot.getSizeInventory(); i++) {
                    inputSlot.setInventorySlotContents(i, null);
                }
                for (int i = 0; i < outputSlot.getSizeInventory(); i++) {
                    outputSlot.setInventorySlotContents(i, null);
                }
                inputs = NEIUtils.clearNull(inputs);
                outputs = NEIUtils.clearNull(outputs);
                adapter.transferPack(inputs, inputSlot);
                adapter.transferPack(outputs, outputSlot);
                c.onCraftMatrixChanged(inputSlot);
                c.onCraftMatrixChanged(outputSlot);
                c.getPatternTerminal()
                    .saveChanges();
            }
        };

        AE2ThingAPI.instance()
            .terminal()
            .registerPatternTerminal(
                new FCPatternTerminal(ContainerPatternTerm.class).registerIdentifier(Constants.NEI_BR, handler));
        AE2ThingAPI.instance()
            .terminal()
            .registerPatternTerminal(
                new FCPatternTerminal(ContainerPatternTermEx.class).registerIdentifier(Constants.NEI_BR, handler));
        AE2ThingAPI.instance()
            .terminal()
            .registerPatternTerminal(new THDualInterfacePatternTerminal())
            .registerIdentifier(Constants.NEI_BR, (container, inputs, outputs, identifier, adapter, message) -> {
                if (container instanceof ContainerWirelessDualInterfaceTerminal ciw) {
                    IPatternTerminal pt = ciw.getContainer()
                        .getPatternTerminal();
                    pt.setCraftingRecipe(false);
                    IInventory inputSlot = pt.getInventoryByName(adapter.getCraftingInvName());
                    IInventory outputSlot = pt.getInventoryByName(adapter.getOutputInvName());
                    for (int i = 0; i < inputSlot.getSizeInventory(); i++) {
                        inputSlot.setInventorySlotContents(i, null);
                    }
                    for (int i = 0; i < outputSlot.getSizeInventory(); i++) {
                        outputSlot.setInventorySlotContents(i, null);
                    }
                    inputs = NEIUtils.clearNull(inputs);
                    outputs = NEIUtils.clearNull(outputs);
                    adapter.transferPack(inputs, inputSlot);
                    adapter.transferPack(outputs, outputSlot);
                    ciw.onCraftMatrixChanged(inputSlot);
                    ciw.onCraftMatrixChanged(outputSlot);
                    ciw.saveChanges();
                }
            });

    }

    private static void putAEStack(final IAEStackInventory inventory, final int index, final IAEStack<?> stack) {
        if (index < 0 || index >= inventory.getSizeInventory()) {
            return;
        }
        inventory.putAEStackInSlot(index, stack);
    }
}
