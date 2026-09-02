package com.asdflj.ae2thing.inventory.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.p455w0rd.wirelesscraftingterminal.api.IWirelessCraftingTermHandler;
import net.p455w0rd.wirelesscraftingterminal.client.gui.GuiWirelessCraftingTerminal;
import net.p455w0rd.wirelesscraftingterminal.common.container.ContainerWirelessCraftingTerminal;
import net.p455w0rd.wirelesscraftingterminal.helpers.WTCGuiObject;
import net.p455w0rd.wirelesscraftingterminal.items.ItemWirelessCraftingTerminal;

import com.asdflj.ae2thing.api.adapter.terminal.item.InventoryPlayerWrapper;

import appeng.api.AEApi;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;

final class WCTGuiFactory extends ItemGuiBridge<ItemWirelessCraftingTerminal> {

    WCTGuiFactory() {
        super(ItemWirelessCraftingTerminal.class);
    }

    @Override
    protected Object createServerGui(EntityPlayer player, ItemWirelessCraftingTerminal inv, ItemStack item) {
        final IWirelessCraftingTermHandler wh = (IWirelessCraftingTermHandler) AEApi.instance()
            .registries()
            .wireless()
            .getWirelessTerminalHandler(item);
        if (wh == null) {
            return null;
        }
        final WTCGuiObject term = new WTCGuiObject(
            wh,
            item,
            player,
            player.worldObj,
            (int) player.posX,
            (int) player.posY,
            (int) player.posZ,
            player.inventory.currentItem);
        AEBaseContainer bc = new ContainerWirelessCraftingTerminal(new InventoryPlayerWrapper(player, item), term);
        bc.setOpenContext(new ContainerOpenContext(term));
        bc.getOpenContext()
            .setWorld(player.worldObj);
        bc.getOpenContext()
            .setX((int) player.posX);
        bc.getOpenContext()
            .setY((int) player.posY);
        bc.getOpenContext()
            .setZ((int) player.posZ);
        bc.getOpenContext()
            .setSide(ForgeDirection.UNKNOWN);
        return bc;
    }

    @Override
    protected Object createClientGui(EntityPlayer player, ItemWirelessCraftingTerminal inv, ItemStack item) {
        if (item == null) return null;
        final WTCGuiObject term = ContainerWirelessCraftingTerminal.getGuiObject(
            item,
            player,
            player.worldObj,
            (int) player.posX,
            (int) player.posY,
            (int) player.posZ,
            player.inventory.currentItem);
        return new GuiWirelessCraftingTerminal(new InventoryPlayerWrapper(player, item), term);
    }
}
