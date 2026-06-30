package com.asdflj.ae2thing.integration.ae2stuff;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

import appeng.api.networking.IGrid;

public class NoOpWirelessConnectorBackend implements WirelessConnectorBackend {

    @Override
    public void writeTiles(EntityPlayer player, IGrid grid, NBTTagList output) {}

    @Override
    public void setName(EntityPlayer player, IGrid grid, String name, NBTTagCompound tag) {}

    @Override
    public void bind(EntityPlayer player, IGrid grid, NBTTagCompound tag) {}

    @Override
    public void unbind(EntityPlayer player, IGrid grid, NBTTagCompound tag) {}

    @Override
    public void setColor(EntityPlayer player, IGrid grid, NBTTagCompound tag) {}

    @Override
    public boolean isWirelessTile(TileEntity tile) {
        return false;
    }
}
