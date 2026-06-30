package com.asdflj.ae2thing.client.gui.container;

import java.util.ArrayList;
import java.util.List;

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

    private final List<NBTTagCompound> wirelessTiles = new ArrayList<>();
    private final WirelessConnectorBackend backend = Ae2StuffIntegration.wirelessConnectorBackend();

    public ContainerWirelessConnectorTerminal(InventoryPlayer ip, ITerminalHost host) {
        super(ip, host);
    }

    public void updateData() {
        if (Platform.isServer()) {
            wirelessTiles.clear();
            if (!hasPower) return;
            if (this.getGrid() == null) return;
            NBTTagList tiles = new NBTTagList();
            this.backend.collectTiles(player, this.getGrid(), tiles);
            for (int i = 0; i < tiles.tagCount(); i++) {
                if (tiles.getCompoundTagAt(i) != null) {
                    wirelessTiles.add(tiles.getCompoundTagAt(i));
                }
            }
            sendToPlayer();
        }
    }

    private void sendToPlayer() {
        NBTTagCompound data = new NBTTagCompound();
        this.writeToNBT(data);
        AE2Thing.proxy.netHandler.sendTo(new SPacketNBTDataUpdate(data), (EntityPlayerMP) player);
    }

    private void writeToNBT(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        tag.setTag(Constants.TILE_ENTRIES, list);
        for (NBTTagCompound tile : this.wirelessTiles) {
            list.appendTag(tile);
        }
    }

    @Override
    public void addCraftingToCrafters(ICrafting crafting) {
        super.addCraftingToCrafters(crafting);
        updateData();
    }

    public void setName(String name, NBTTagCompound tag) {
        this.backend.setName(player, this.getGrid(), name, tag);
        updateData();
    }

    public void bind(NBTTagCompound tag) {
        this.backend.bind(player, this.getGrid(), tag);
        updateData();
    }

    public void unBind(NBTTagCompound tag) {
        this.backend.unbind(player, this.getGrid(), tag);
        updateData();
    }

    public void setColor(NBTTagCompound tag) {
        this.backend.setColor(player, this.getGrid(), tag);
        updateData();
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
