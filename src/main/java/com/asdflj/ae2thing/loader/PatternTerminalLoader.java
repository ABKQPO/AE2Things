package com.asdflj.ae2thing.loader;

import net.minecraft.inventory.IInventory;

import com.asdflj.ae2thing.api.AE2ThingAPI;
import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.api.adapter.pattern.THDualInterfacePatternTerminal;
import com.asdflj.ae2thing.client.gui.container.ContainerInfusionPatternTerminal;
import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.inventory.IPatternTerminal;
import com.asdflj.ae2thing.nei.NEIUtils;

import appeng.api.storage.StorageName;
import appeng.tile.inventory.IAEStackInventory;

public class PatternTerminalLoader implements Runnable {

    @Override
    public void run() {
        AE2ThingAPI.instance()
            .terminal()
            .registerPatternTerminal(new THDualInterfacePatternTerminal())
            .registerIdentifier(Constants.NEI_DEFAULT, (container, inputs, outputs, identifier, adapter, message) -> {
                if (container instanceof ContainerWirelessDualInterfaceTerminal ciw) {
                    boolean combine = ciw.combine;
                    ciw.setCraftingMode(message.isCraft);
                    ciw.setCrafting(message.isCraft);
                    IPatternTerminal pt = ciw.getContainer()
                        .getPatternTerminal();
                    if (!message.isCraft) {
                        final appeng.api.parts.IPatternTerminal terminal = (appeng.api.parts.IPatternTerminal) pt;
                        final IAEStackInventory inputsInv = terminal.getAEInventoryByName(StorageName.CRAFTING_INPUT);
                        final IAEStackInventory outputsInv = terminal.getAEInventoryByName(StorageName.CRAFTING_OUTPUT);
                        if (inputsInv == null || outputsInv == null) return;
                        for (int i = 0; i < inputsInv.getSizeInventory(); i++) {
                            inputsInv.putAEStackInSlot(i, null);
                        }
                        for (int i = 0; i < outputsInv.getSizeInventory(); i++) {
                            outputsInv.putAEStackInSlot(i, null);
                        }
                        if (combine) {
                            inputs = NEIUtils.compress(inputs);
                            outputs = NEIUtils.compress(outputs);
                        }
                        inputs = NEIUtils.clearNull(inputs);
                        outputs = NEIUtils.clearNull(outputs);
                        adapter.transferPackAE(inputs, inputsInv);
                        adapter.transferPackAE(outputs, outputsInv);
                        ciw.saveChanges();
                        return;
                    }
                    IInventory inputSlot = pt.getInventoryByName(Constants.CRAFTING);
                    if (inputSlot == null) return;
                    final IAEStackInventory outputsInv = ((appeng.api.parts.IPatternTerminal) pt)
                        .getAEInventoryByName(StorageName.CRAFTING_OUTPUT);
                    for (int i = 0; i < inputSlot.getSizeInventory(); i++) {
                        inputSlot.setInventorySlotContents(i, null);
                    }
                    if (outputsInv != null) {
                        for (int i = 0; i < outputsInv.getSizeInventory(); i++) {
                            outputsInv.putAEStackInSlot(i, null);
                        }
                    }
                    adapter.transferPack(inputs, inputSlot);
                    ciw.onCraftMatrixChanged(inputSlot);
                    ciw.saveChanges();
                }
            });
        if (Mods.THAUMIC_ENERGISTICS.isModLoaded()) {
            AE2ThingAPI.instance()
                .terminal()
                .registerPatternTerminal(() -> ContainerInfusionPatternTerminal.class)
                .registerIdentifier(
                    Constants.NEI_DEFAULT,
                    (container, inputs, outputs, identifier, adapter, message) -> {
                        if (container instanceof ContainerInfusionPatternTerminal ct) {
                            // pattern terminal only
                            boolean combine = ct.combine;
                            ct.getPatternTerminal()
                                .setCraftingRecipe(true);
                            ct.setCrafting(true);
                            IInventory inputSlot = ct.getInventoryByName(Constants.CRAFTING);
                            IInventory outputSlot = ct.getInventoryByName(Constants.OUTPUT);
                            for (int i = 0; i < inputSlot.getSizeInventory(); i++) {
                                inputSlot.setInventorySlotContents(i, null);
                            }
                            for (int i = 0; i < outputSlot.getSizeInventory(); i++) {
                                outputSlot.setInventorySlotContents(i, null);
                            }
                            if (combine) {
                                inputs = NEIUtils.compress(inputs);
                                outputs = NEIUtils.compress(outputs);
                            }
                            inputs = NEIUtils.clearNull(inputs);
                            outputs = NEIUtils.clearNull(outputs);
                            adapter.transferPack(inputs, inputSlot);
                            adapter.transferPack(outputs, outputSlot);
                            ct.onCraftMatrixChanged(inputSlot);
                            ct.onCraftMatrixChanged(outputSlot);
                            ct.saveChanges();
                        }
                    });
        }
    }
}
