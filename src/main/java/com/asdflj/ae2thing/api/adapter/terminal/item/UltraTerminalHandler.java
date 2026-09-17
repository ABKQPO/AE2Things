package com.asdflj.ae2thing.api.adapter.terminal.item;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.util.BaublesUtil;
import com.glodblock.github.common.item.ItemBaseWirelessTerminal;
import com.glodblock.github.common.item.ItemWirelessUltraTerminal;
import com.glodblock.github.inventory.InventoryHandler;
import com.glodblock.github.inventory.gui.GuiType;
import com.glodblock.github.util.BlockPos;
import com.glodblock.github.util.UltraTerminalModes;

import appeng.api.AEApi;
import appeng.core.AppEng;
import appeng.core.sync.GuiBridge;
import appeng.util.Platform;

public class UltraTerminalHandler implements ITerminalHandler {

    @Override
    public void openGui(ItemStack item, ITerminalHandler terminal, TerminalItems terminalItems, EntityPlayerMP player) {
        if (item == null) return;
        if (!(item.getItem() instanceof ItemWirelessUltraTerminal)) return;
        final UltraTerminalModes mode = ItemBaseWirelessTerminal.getMode(terminalItems.getTargetItem());
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (Platform.isSameItemPrecise(stack, item)) {
                switchTerminal(player, stack, i, mode);
                return;
            }
        }
        if (!Mods.BAUBLES.isModLoaded()) return;
        IInventory handler = BaublesUtil.getBaublesInv(player);
        if (handler == null) return;
        for (int i = 0; i < handler.getSizeInventory(); ++i) {
            ItemStack is = handler.getStackInSlot(i);
            if (BaublesUtil.isSameItemPrecise(is, item, i, terminalItems)) {
                switchTerminal(player, is, BaublesUtil.toAESlotIndex(i), mode);
                BaublesUtil.syncBaubles(player, i);
                return;
            }
        }
    }

    private void switchTerminal(EntityPlayerMP player, ItemStack stack, int slotIndex, UltraTerminalModes mode) {
        ItemBaseWirelessTerminal.setMode(stack, mode);
        if (!AEApi.instance()
            .registries()
            .wireless()
            .performCheck(stack, player)) {
            return;
        }
        if (mode == UltraTerminalModes.LEVEL) {
            InventoryHandler.openGui(
                player,
                player.worldObj,
                new BlockPos(slotIndex, 0, 0),
                ForgeDirection.UNKNOWN,
                GuiType.WIRELESS_LEVEL_TERMINAL);
            return;
        }
        player.openGui(
            AppEng.instance(),
            guiOf(mode).ordinal() << 5 | (1 << 4),
            player.getEntityWorld(),
            Platform.itemGuiSlotOffset + slotIndex,
            mode.ordinal(),
            0);
    }

    private static GuiBridge guiOf(UltraTerminalModes mode) {
        return switch (mode) {
            case CRAFTING -> GuiBridge.GUI_CRAFTING_TERMINAL;
            case PATTERN -> GuiBridge.GUI_PATTERN_TERMINAL;
            case PATTERN_EX -> GuiBridge.GUI_PATTERN_TERMINAL_EX;
            case INTERFACE -> GuiBridge.GUI_INTERFACE_TERMINAL;
            default -> GuiBridge.GUI_ME;
        };
    }
}
