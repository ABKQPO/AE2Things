package com.asdflj.ae2thing.api.adapter.terminal.item;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.asdflj.ae2thing.util.BaublesUtil;
import com.asdflj.ae2thing.util.ModAndClassUtil;
import com.glodblock.github.common.item.ItemBaseWirelessTerminal;
import com.glodblock.github.common.item.ItemWirelessUltraTerminal;
import com.glodblock.github.util.UltraTerminalModes;

import appeng.util.Platform;

public class UltraTerminalHandler implements ITerminalHandler {

    @Override
    public void openGui(ItemStack item, ITerminalHandler terminal, TerminalItems terminalItems, EntityPlayerMP player) {
        if (item == null) return;
        if (!(item.getItem() instanceof ItemWirelessUltraTerminal ultra)) return;
        final UltraTerminalModes mode = ItemBaseWirelessTerminal.getMode(terminalItems.getTargetItem());
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (Platform.isSameItemPrecise(stack, item)) {
                ultra.switchTerminal(player, new ImmutablePair<>(i, stack), mode);
                return;
            }
        }
        if (!ModAndClassUtil.BAUBLES) return;
        IInventory handler = BaublesUtil.getBaublesInv(player);
        if (handler == null) return;
        for (int i = 0; i < handler.getSizeInventory(); ++i) {
            ItemStack is = handler.getStackInSlot(i);
            if (BaublesUtil.isSameItemPrecise(is, item, i, terminalItems)) {
                ultra.switchTerminal(player, new ImmutablePair<>(i, is), mode);
                return;
            }
        }
    }
}
