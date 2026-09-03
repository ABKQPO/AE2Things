package com.asdflj.ae2thing.common.parts;

import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import com.asdflj.ae2thing.client.textures.BlockTexture;
import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.inventory.item.INetworkTerminal;

import appeng.api.networking.IGrid;
import appeng.me.GridAccessException;

public class PartWirelessConnectorTerminal extends THPart implements INetworkTerminal {

    public PartWirelessConnectorTerminal(ItemStack is) {
        super(is, true);
    }

    @Override
    public GuiType getGui() {
        return GuiType.WIRELESS_CONNECTOR_TERMINAL;
    }

    @Override
    public IIcon getFrontBright() {
        return BlockTexture.WirelessConnectorTerminal_Bright.getIcon();
    }

    @Override
    public IIcon getFrontColored() {
        return BlockTexture.WirelessConnectorTerminal_Medium.getIcon();
    }

    @Override
    public IIcon getFrontDark() {
        return BlockTexture.WirelessConnectorTerminal_Dark.getIcon();
    }

    @Override
    public boolean isLightSource() {
        return false;
    }

    @Override
    public IGrid getGrid() {
        try {
            return this.proxy.getGrid();
        } catch (GridAccessException e) {
            return null;
        }
    }
}
