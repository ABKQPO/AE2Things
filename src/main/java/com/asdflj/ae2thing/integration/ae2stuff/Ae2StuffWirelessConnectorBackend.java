package com.asdflj.ae2thing.integration.ae2stuff;

import net.bdew.ae2stuff.grid.Security;
import net.bdew.ae2stuff.machines.wireless.BlockWireless;
import net.bdew.ae2stuff.machines.wireless.TileWireless;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.util.Util;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IMachineSet;
import appeng.api.util.AEColor;
import appeng.api.util.DimensionalCoord;
import appeng.hooks.TickHandler;
import appeng.me.Grid;
import scala.Option;

public class Ae2StuffWirelessConnectorBackend implements WirelessConnectorBackend {

    @Override
    public void writeTiles(EntityPlayer player, IGrid grid, NBTTagList output) {
        visitAvailableTiles(player, grid, tile -> {
            output.appendTag(writeTile(tile));
            return false;
        });
    }

    @Override
    public void setName(EntityPlayer player, IGrid grid, String name, NBTTagCompound tag) {
        if (tag == null) {
            return;
        }
        TileWireless tile = findTile(player, grid, DimensionalCoord.readFromNBT(tag), null);
        if (tile != null) {
            tile.setCustomName(name);
        }
    }

    @Override
    public void bind(EntityPlayer player, IGrid grid, NBTTagCompound tag) {
        if (tag == null) {
            return;
        }
        DimensionalCoord leftCoord = DimensionalCoord.readFromNBT((NBTTagCompound) tag.getTag("#0"));
        DimensionalCoord rightCoord = DimensionalCoord.readFromNBT((NBTTagCompound) tag.getTag("#1"));
        TileWireless[] tiles = findTiles(player, grid, leftCoord, rightCoord);
        if (tiles[0] != null && tiles[1] != null) {
            link(player, tiles[0], tiles[1]);
        }
    }

    @Override
    public void unbind(EntityPlayer player, IGrid grid, NBTTagCompound tag) {
        if (tag == null) {
            return;
        }
        DimensionalCoord coord = DimensionalCoord.readFromNBT((NBTTagCompound) tag.getTag("#0"));
        TileWireless tile = findTile(player, grid, coord, null);
        if (tile != null && tile.isLinked()) {
            tile.doUnlink();
        }
    }

    @Override
    public void setColor(EntityPlayer player, IGrid grid, NBTTagCompound tag) {
        if (tag == null) {
            return;
        }
        NBTTagCompound data = (NBTTagCompound) tag.getTag("#0");
        DimensionalCoord coord = DimensionalCoord.readFromNBT(data);
        int colorIndex = data.getShort(Constants.COLOR);
        if (colorIndex < 0 || colorIndex >= AEColor.values().length) {
            return;
        }
        TileWireless tile = findTile(player, grid, coord, null);
        if (tile != null) {
            tile.color_$eq(AEColor.values()[colorIndex]);
            tile.getWorldObject()
                .markBlockForUpdate(coord.x, coord.y, coord.z);
        }
    }

    @Override
    public boolean isWirelessTile(TileEntity tile) {
        return tile instanceof TileWireless;
    }

    private void visitAvailableTiles(EntityPlayer player, IGrid currentGrid, TileVisitor visitor) {
        if (player == null || currentGrid == null) {
            return;
        }
        int playerID = Security.getPlayerId(player.getGameProfile());
        for (Grid grid : TickHandler.INSTANCE.getGridList()) {
            IMachineSet set = grid.getMachines(TileWireless.class);
            if (set.isEmpty()) {
                continue;
            }
            boolean sameGrid = currentGrid.equals(grid);
            for (IGridNode node : set) {
                TileWireless tile = (TileWireless) node.getGridBlock();
                if (sameGrid) {
                    if (visitor.visit(tile)) {
                        return;
                    }
                    continue;
                }
                int id = node.getPlayerID();
                if (id == -1 || id != playerID) {
                    continue;
                }
                if (visitor.visit(tile)) {
                    return;
                }
            }
        }
    }

    private TileWireless findTile(EntityPlayer player, IGrid grid, DimensionalCoord coord, TileWireless excluded) {
        TileWireless[] result = new TileWireless[1];
        visitAvailableTiles(player, grid, tile -> {
            if (tile != excluded && Util.isSameDimensionalCoord(tile.getLocation(), coord)) {
                result[0] = tile;
                return true;
            }
            return false;
        });
        return result[0];
    }

    private TileWireless[] findTiles(EntityPlayer player, IGrid grid, DimensionalCoord leftCoord,
        DimensionalCoord rightCoord) {
        TileWireless[] result = new TileWireless[2];
        visitAvailableTiles(player, grid, tile -> {
            if (result[0] == null && Util.isSameDimensionalCoord(tile.getLocation(), leftCoord)) {
                result[0] = tile;
            } else if (result[1] == null && Util.isSameDimensionalCoord(tile.getLocation(), rightCoord)) {
                result[1] = tile;
            }
            return result[0] != null && result[1] != null;
        });
        if (result[0] != null && result[0] == result[1]) {
            result[1] = findTile(player, grid, rightCoord, result[0]);
        }
        return result;
    }

    private NBTTagCompound writeTile(TileWireless tile) {
        NBTTagCompound data = new NBTTagCompound();
        tile.getLocation()
            .writeToNBT(data);
        data.setString(Constants.NAME, tile.hasCustomName() ? tile.getCustomName() : BlockWireless.getLocalizedName());
        data.setInteger(
            Constants.COLOR,
            tile.getColor()
                .ordinal());
        data.setBoolean(Constants.IS_LINKED, tile.isLinked());
        data.setInteger(
            Constants.USED_CHANNELS,
            tile.connection() != null ? tile.connection()
                .getUsedChannels() : 0);
        if (tile.isLinked()) {
            NBTTagCompound linked = new NBTTagCompound();
            Option<TileWireless> other = tile.getLink();
            if (!other.isEmpty()) {
                other.get()
                    .getLocation()
                    .writeToNBT(linked);
                data.setTag(Constants.LINK, linked);
            }
        }
        return data;
    }

    private void link(EntityPlayer player, TileWireless left, TileWireless right) {
        if (left == right) {
            return;
        }
        if (left.isLinked()) {
            left.doUnlink();
        }
        if (right.isLinked()) {
            right.doUnlink();
        }
        try {
            left.doLink(right);
        } catch (Exception e) {
            left.doUnlink();
            right.doUnlink();
            ChatComponentText message = new ChatComponentText(
                StatCollector.translateToLocal("ae2stuff.wireless.tool.failed") + ": " + e.getMessage());
            message.getChatStyle()
                .setColor(EnumChatFormatting.RED);
            player.addChatComponentMessage(message);
        }
    }

    @FunctionalInterface
    private interface TileVisitor {

        boolean visit(TileWireless tile);
    }
}
