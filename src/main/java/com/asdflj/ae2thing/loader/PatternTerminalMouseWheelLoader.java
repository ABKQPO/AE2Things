package com.asdflj.ae2thing.loader;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

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
import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;

public class PatternTerminalMouseWheelLoader implements Runnable {

    @Override
    public void run() {
        IRecipeHandler handler = (container, inputs, outputs, _, adapter, _) -> {
            if (container instanceof IAEAppEngInventory inventory) {
                ItemStack in = (ItemStack) inputs.getFirst()
                    .getStack();
                ItemStack out = (ItemStack) outputs.getFirst()
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
            .registerIdentifier(Constants.NEI_MOUSE_WHEEL, (container, inputs, outputs, _, _, _) -> {
                if (container instanceof ContainerWirelessDualInterfaceTerminal c) {
                    Object in = inputs.getFirst()
                        .getStack();
                    Object out = outputs.getFirst()
                        .getStack();
                    final IPatternTerminal pt = c.getContainer()
                        .getPatternTerminal();
                    if (in instanceof FluidStack fluidIn && out instanceof FluidStack fluidOut) {
                        if (pt.isCraftingRecipe()) {
                            final IInventory inv = pt.getInventoryByName(Constants.CRAFTING);
                            if (inv != null) {
                                final ItemStack inPacket = ItemFluidPacket.newStack(fluidIn);
                                final ItemStack outPacket = ItemFluidPacket.newStack(fluidOut);
                                for (int i = 0; i < inv.getSizeInventory(); i++) {
                                    if (Platform.isSameItemPrecise(inv.getStackInSlot(i), inPacket)) {
                                        inv.setInventorySlotContents(i, outPacket);
                                    }
                                }
                                container.onCraftMatrixChanged(inv);
                            }
                        } else {
                            final IAEStackInventory inv = ((appeng.api.parts.IPatternTerminal) pt)
                                .getAEInventoryByName(StorageName.CRAFTING_INPUT);
                            if (inv != null) {
                                final IAEFluidStack target = AEFluidStack.create(fluidIn);
                                final IAEFluidStack replacement = AEFluidStack.create(fluidOut);
                                for (int i = 0; i < inv.getSizeInventory(); i++) {
                                    final IAEStack<?> current = inv.getAEStackInSlot(i);
                                    if (current != null && target.isSameType(current)) {
                                        inv.putAEStackInSlot(i, replacement);
                                    }
                                }
                            }
                        }
                    } else if (in instanceof ItemStack itemIn && out instanceof ItemStack itemOut) {
                        if (pt.isCraftingRecipe()) {
                            final IInventory inv = pt.getInventoryByName(Constants.CRAFTING);
                            if (inv != null) {
                                for (int i = 0; i < inv.getSizeInventory(); i++) {
                                    if (Platform.isSameItemPrecise(inv.getStackInSlot(i), itemIn)) {
                                        inv.setInventorySlotContents(i, itemOut);
                                    }
                                }
                                container.onCraftMatrixChanged(inv);
                            }
                        } else {
                            final IAEStackInventory inv = ((appeng.api.parts.IPatternTerminal) pt)
                                .getAEInventoryByName(StorageName.CRAFTING_INPUT);
                            final IAEItemStack target = AEItemStack.create(itemIn);
                            if (inv != null) {
                                final IAEStack<?> replacement = AEStackItemInventory.toAEStack(itemOut);
                                for (int i = 0; i < inv.getSizeInventory(); i++) {
                                    final IAEStack<?> current = inv.getAEStackInSlot(i);
                                    if (current instanceof IAEItemStack item && target.isSameType(item)) {
                                        inv.putAEStackInSlot(i, replacement);
                                    }
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
                .registerIdentifier(Constants.NEI_MOUSE_WHEEL, (container, inputs, outputs, _, _, _) -> {
                    if (container instanceof ContainerInfusionPatternTerminal ct) {
                        ct.getPatternTerminal()
                            .setCraftingRecipe(true);
                        ct.setCrafting(true);
                        IInventory outputSlot = ct.getInventoryByName(Constants.OUTPUT);
                        ItemStack in = (ItemStack) inputs.getFirst()
                            .getStack();
                        ItemStack out = (ItemStack) outputs.getFirst()
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
