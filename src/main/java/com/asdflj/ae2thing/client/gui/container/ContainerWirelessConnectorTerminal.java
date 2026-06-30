package com.asdflj.ae2thing.client.gui.container;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.integration.ae2stuff.Ae2StuffIntegration;
import com.asdflj.ae2thing.integration.ae2stuff.WirelessConnectorBackend;
import com.asdflj.ae2thing.inventory.item.INetworkTerminal;
import com.asdflj.ae2thing.network.SPacketNBTDataUpdate;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.storage.ITerminalHost;
import appeng.util.Platform;

public class ContainerWirelessConnectorTerminal extends BaseNetworkContainer implements INetworkTerminal {

    private final WirelessConnectorBackend backend = Ae2StuffIntegration.wirelessConnectorBackend();

    public ContainerWirelessConnectorTerminal(InventoryPlayer ip, ITerminalHost host) {
        super(ip, host);
    }

    public void updateData() {
        if (Platform.isServer()) {
            if (!hasPower) return;
            IGrid grid = this.getGrid();
            if (grid == null) return;
            sendToPlayer(grid);
        }
    }

    private void sendToPlayer(IGrid grid) {
        NBTTagCompound data = new NBTTagCompound();
        this.writeToNBT(data, grid);
        AE2Thing.proxy.netHandler.sendTo(new SPacketNBTDataUpdate(data), (EntityPlayerMP) player);
    }

    private void writeToNBT(NBTTagCompound tag, IGrid grid) {
        NBTTagList list = new NBTTagList();
        tag.setTag(Constants.TILE_ENTRIES, list);
        this.backend.writeTiles(player, grid, list);
    }

    @Override
    public void addCraftingToCrafters(ICrafting crafting) {
        super.addCraftingToCrafters(crafting);
        updateData();
    }

    public void setName(String name, NBTTagCompound tag) {
        IGrid grid = this.getGrid();
        this.backend.setName(player, grid, name, tag);
        updateData(grid);
    }

    public void bind(NBTTagCompound tag) {
        IGrid grid = this.getGrid();
        this.backend.bind(player, grid, tag);
        updateData(grid);
    }

    public void unBind(NBTTagCompound tag) {
        IGrid grid = this.getGrid();
        this.backend.unbind(player, grid, tag);
        updateData(grid);
    }

    public void setColor(NBTTagCompound tag) {
        IGrid grid = this.getGrid();
        this.backend.setColor(player, grid, tag);
        updateData(grid);
    }

    private void updateData(IGrid grid) {
        if (Platform.isServer() && hasPower && grid != null) {
            sendToPlayer(grid);
        }
    }

    @Override
    public IGrid getGrid() {
        return getGridNode().getGrid();
    }

    @Override
    public IGridNode getGridNode() {
        return this.getActionHost()
            .getActionableNode();
    }
}
