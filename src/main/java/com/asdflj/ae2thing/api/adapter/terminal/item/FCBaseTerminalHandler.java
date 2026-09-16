package com.asdflj.ae2thing.api.adapter.terminal.item;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.util.BaublesUtil;
import com.glodblock.github.common.item.ItemBaseWirelessTerminal;
import com.glodblock.github.common.item.ItemWirelessInterfaceTerminal;
import com.glodblock.github.common.item.ItemWirelessLevelTerminal;
import com.glodblock.github.common.item.ItemWirelessPatternTerminal;
import com.glodblock.github.inventory.InventoryHandler;
import com.glodblock.github.inventory.gui.GuiType;
import com.glodblock.github.util.BlockPos;

import appeng.container.AEBaseContainer;
import appeng.core.sync.GuiBridge;
import appeng.util.Platform;

public class FCBaseTerminalHandler implements ITerminalHandler {

    @Override
    public void openGui(ItemStack item, ITerminalHandler terminal, TerminalItems terminalItems, EntityPlayerMP player) {
        if (item == null) return;
        if (!(item.getItem() instanceof ItemBaseWirelessTerminal base)) return;
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (Platform.isSameItemPrecise(stack, item)) {
                openGui(base, player, i);
                return;
            }
        }
        if (!Mods.BAUBLES.isModLoaded()) return;
        IInventory handler = BaublesUtil.getBaublesInv(player);
        if (handler == null) return;
        for (int i = 0; i < handler.getSizeInventory(); ++i) {
            ItemStack is = handler.getStackInSlot(i);
            if (BaublesUtil.isSameItemPrecise(is, item, i, terminalItems)) {
                openGui(base, player, BaublesUtil.toAESlotIndex(i));
                return;
            }
        }
    }

    private void openGui(ItemBaseWirelessTerminal item, EntityPlayerMP player, int slotIndex) {
        if (item instanceof ItemWirelessLevelTerminal) {
            InventoryHandler.openGui(
                player,
                player.worldObj,
                new BlockPos(
                    slotIndex,
                    0,
                    player.openContainer instanceof AEBaseContainer abc ? abc.getSwitchAbleGuiNext() : 0),
                ForgeDirection.UNKNOWN,
                GuiType.WIRELESS_LEVEL_TERMINAL);
            return;
        }
        final GuiBridge bridge;
        if (item instanceof ItemWirelessPatternTerminal) {
            bridge = GuiBridge.GUI_PATTERN_TERMINAL;
        } else if (item instanceof ItemWirelessInterfaceTerminal) {
            bridge = GuiBridge.GUI_INTERFACE_TERMINAL;
        } else {
            return;
        }
        Platform.openGUI(player, null, null, bridge, slotIndex);
    }
}
