package com.asdflj.ae2thing.integration.ae2stuff;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

import appeng.api.networking.IGrid;

public interface WirelessConnectorBackend {

    void writeTiles(EntityPlayer player, IGrid grid, NBTTagList output);

    void setName(EntityPlayer player, IGrid grid, String name, NBTTagCompound tag);

    void bind(EntityPlayer player, IGrid grid, NBTTagCompound tag);

    void unbind(EntityPlayer player, IGrid grid, NBTTagCompound tag);

    void setColor(EntityPlayer player, IGrid grid, NBTTagCompound tag);

    boolean isWirelessTile(TileEntity tile);
}
