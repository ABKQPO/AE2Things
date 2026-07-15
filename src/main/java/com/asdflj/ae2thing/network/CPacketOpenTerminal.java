package com.asdflj.ae2thing.network;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import com.asdflj.ae2thing.api.AE2ThingAPI;
import com.asdflj.ae2thing.api.adapter.terminal.item.ITerminalHandler;
import com.asdflj.ae2thing.api.adapter.terminal.item.TerminalItems;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

public class CPacketOpenTerminal implements IMessage {

    private TerminalItems terminalItems;

    public CPacketOpenTerminal() {}

    public CPacketOpenTerminal(TerminalItems terminal) {
        this.terminalItems = terminal;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        try {
            final NBTTagCompound comp = PacketDecodeUtil.readCompressedNbt(buf);
            if (comp != null) {
                this.terminalItems = TerminalItems.readFromNBT(comp);
            }
        } catch (IOException e) {
            throw new DecoderException("Failed to decode terminal request", e);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        try {
            NBTTagCompound tag = new NBTTagCompound();
            this.terminalItems.writeNBT(tag);
            final ByteBuf data = Unpooled.buffer();
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            final DataOutputStream outputStream = new DataOutputStream(bytes);

            CompressedStreamTools.writeCompressed(tag, outputStream);
            data.writeBytes(bytes.toByteArray());
            data.capacity(data.readableBytes());
            buf.writeBytes(data);

        } catch (IOException e) {
            throw new EncoderException("Failed to encode terminal request", e);
        }
    }

    public static class Handler implements IMessageHandler<CPacketOpenTerminal, IMessage> {

        @Nullable
        @Override
        public IMessage onMessage(CPacketOpenTerminal message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (message.terminalItems == null) return null;
            ItemStack item = message.terminalItems.getRawItem();
            if (item == null || item.getItem() == null) return null;
            ITerminalHandler terminal = AE2ThingAPI.instance()
                .terminal()
                .getOpenTerminalHandler(
                    item.getItem()
                        .getClass());
            if (terminal == null) return null;
            if (terminal.canConnect(message.terminalItems.getRawItem(), terminal, message.terminalItems, player)) {
                terminal.openGui(message.terminalItems.getRawItem(), terminal, message.terminalItems, player);
            }

            return null;
        }
    }
}
