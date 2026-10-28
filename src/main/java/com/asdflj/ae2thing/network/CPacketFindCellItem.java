package com.asdflj.ae2thing.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.api.AE2ThingAPI;
import com.asdflj.ae2thing.api.adapter.findit.IFindItAdapter;
import com.asdflj.ae2thing.util.StorageProvider;
import com.asdflj.ae2thing.util.Util;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.data.IAEStack;
import appeng.container.AEBaseContainer;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

public class CPacketFindCellItem implements IMessage {

    public IAEStack<?> request;

    @SuppressWarnings("unused")
    public CPacketFindCellItem() {}

    public CPacketFindCellItem(ItemStack is) {
        this.request = Util.toAEStack(is);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        try {
            this.request = IAEStack.fromPacketGeneric(buf);
        } catch (Exception e) {
            throw new DecoderException("Failed to decode Find Cell request", e);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        try {
            IAEStack.writeToPacketGeneric(buf, this.request);
        } catch (Exception e) {
            throw new EncoderException("Failed to encode Find Cell request", e);
        }
    }

    public static class Handler implements IMessageHandler<CPacketFindCellItem, IMessage> {

        @Override
        public IMessage onMessage(CPacketFindCellItem message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (message.request != null && player.openContainer instanceof AEBaseContainer container
                && container.getActionSource() instanceof PlayerSource ps) {
                List<StorageProvider> posList = new ArrayList<>();
                IGrid iGrid = null;
                try {
                    iGrid = ps.via.getActionableNode()
                        .getGrid();
                } catch (Exception ignored) {}
                if (iGrid == null) return null;
                Collection<IFindItAdapter> adapters = AE2ThingAPI.instance()
                    .terminal()
                    .getStorageProviders();
                for (IFindItAdapter adapter : adapters) {
                    posList.addAll(adapter.getStorageProviders(iGrid, message.request));
                }

                if (!posList.isEmpty()) AE2Thing.proxy.netHandler.sendTo(new SPacketFindCellItem(posList), player);
            }

            return null;
        }
    }
}
