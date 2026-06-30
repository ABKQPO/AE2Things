package com.asdflj.ae2thing.api.adapter.terminal.item;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.glodblock.github.common.item.ItemBaseWirelessTerminal;

import appeng.util.Platform;

public class FCBaseTerminalHandler implements ITerminalHandler {

    @Override
    public void openGui(ItemStack item, ITerminalHandler terminal, TerminalItems items, EntityPlayerMP player) {
        if (item == null) return;
        if (item.getItem() instanceof ItemBaseWirelessTerminal t) {
            for (int i = 0; i < player.inventory.mainInventory.length; i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (Platform.isSameItemPrecise(stack, item)) {
                    t.openGui(stack, player.worldObj, player, null);
                    return;
                }
            }
        }
    }
}
