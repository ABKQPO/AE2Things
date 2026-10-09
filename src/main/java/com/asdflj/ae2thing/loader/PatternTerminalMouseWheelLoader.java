package com.asdflj.ae2thing.loader;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.api.AE2ThingAPI;
import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.api.adapter.pattern.FCPatternTerminal;
import com.asdflj.ae2thing.api.adapter.pattern.IRecipeHandler;
import com.asdflj.ae2thing.api.adapter.pattern.THDualInterfacePatternTerminal;
import com.asdflj.ae2thing.client.gui.container.ContainerInfusionPatternTerminal;
import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.inventory.AEStackItemInventory;
import com.asdflj.ae2thing.inventory.IPatternTerminal;

import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;

public class PatternTerminalMouseWheelLoader implements Runnable {

    @Override
    public void run() {
        IRecipeHandler handler = (container, inputs, outputs, identifier, adapter, message) -> {
            if (container instanceof IAEAppEngInventory inventory) {
                ItemStack in = (ItemStack) inputs.get(0)
                    .getStack();
                ItemStack out = (ItemStack) outputs.get(0)
                    .getStack();
                IInventory inv = adapter.getInventoryByName(container, adapter.getCraftingInvName());
                for (int i = 0; i < inv.getSizeInventory(); i++) {
                    if (Platform.isSameItemPrecise(inv.getStackInSlot(i), in)) {
                        inv.setInventorySlotContents(i, out);
                    }
                }
                container.onCraftMatrixChanged(inv);
                inventory.saveChanges();
            }
        };

        AE2ThingAPI.instance()
            .terminal()
            .registerPatternTerminal(new THDualInterfacePatternTerminal())
            .registerIdentifier(
                Constants.NEI_MOUSE_WHEEL,
                (container, inputs, outputs, identifier, adapter, message) -> {
                    if (container instanceof ContainerWirelessDualInterfaceTerminal c) {
                        ItemStack in = (ItemStack) inputs.get(0)
                            .getStack();
                        ItemStack out = (ItemStack) outputs.get(0)
                            .getStack();
                        final IPatternTerminal pt = c.getContainer()
                            .getPatternTerminal();
                        if (pt.isCraftingRecipe()) {
                            final IInventory inv = pt.getInventoryByName(Constants.CRAFTING);
                            if (inv != null) {
                                for (int i = 0; i < inv.getSizeInventory(); i++) {
                                    if (Platform.isSameItemPrecise(inv.getStackInSlot(i), in)) {
                                        inv.setInventorySlotContents(i, out);
                                    }
                                }
                                container.onCraftMatrixChanged(inv);
                            }
                        } else {
                            final IAEStackInventory inv = ((appeng.api.parts.IPatternTerminal) pt)
                                .getAEInventoryByName(StorageName.CRAFTING_INPUT);
                            final IAEItemStack target = AEItemStack.create(in);
                            if (inv != null && target != null) {
                                final IAEStack<?> replacement = AEStackItemInventory.toAEStack(out);
                                for (int i = 0; i < inv.getSizeInventory(); i++) {
                                    final IAEStack<?> current = inv.getAEStackInSlot(i);
                                    if (current instanceof IAEItemStack item && target.isSameType(item)) {
                                        inv.putAEStackInSlot(i, replacement);
                                    }
                                }
                            }
                        }
                        c.saveChanges();
                    }
                });
        AE2ThingAPI.instance()
            .terminal()
            .registerPatternTerminal(new FCPatternTerminal(ContainerPatternTerm.class))
            .registerIdentifier(Constants.NEI_MOUSE_WHEEL, handler);
        AE2ThingAPI.instance()
            .terminal()
            .registerPatternTerminal(new FCPatternTerminal(ContainerPatternTermEx.class))
            .registerIdentifier(Constants.NEI_MOUSE_WHEEL, handler);

        if (Mods.THAUMIC_ENERGISTICS.isModLoaded()) {
            AE2ThingAPI.instance()
                .terminal()
                .registerPatternTerminal(() -> ContainerInfusionPatternTerminal.class)
                .registerIdentifier(
                    Constants.NEI_MOUSE_WHEEL,
                    (container, inputs, outputs, identifier, adapter, message) -> {
                        if (container instanceof ContainerInfusionPatternTerminal ct) {
                            ct.getPatternTerminal()
                                .setCraftingRecipe(true);
                            ct.setCrafting(true);
                            IInventory outputSlot = ct.getInventoryByName(Constants.OUTPUT);
                            ItemStack in = (ItemStack) inputs.get(0)
                                .getStack();
                            ItemStack out = (ItemStack) outputs.get(0)
                                .getStack();
                            for (int i = 0; i < outputSlot.getSizeInventory(); i++) {
                                if (Platform.isSameItemPrecise(outputSlot.getStackInSlot(i), in)) {
                                    outputSlot.setInventorySlotContents(i, out);
                                }
                            }
                            ct.onCraftMatrixChanged(outputSlot);
                            ct.saveChanges();
                        }
                    });
        }
    }

}
