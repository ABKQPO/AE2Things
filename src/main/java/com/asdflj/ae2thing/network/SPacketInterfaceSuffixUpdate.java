package com.asdflj.ae2thing.network;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import com.asdflj.ae2thing.client.gui.GuiWirelessDualInterfaceTerminal;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class SPacketInterfaceSuffixUpdate implements IMessage {

    private Long2ObjectMap<String> suffixes = new Long2ObjectOpenHashMap<>();

    @SuppressWarnings("unused")
    public SPacketInterfaceSuffixUpdate() {}

    public SPacketInterfaceSuffixUpdate(Long2ObjectMap<String> suffixes) {
        this.suffixes = suffixes;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        this.suffixes = new Long2ObjectOpenHashMap<>(size);
        for (int i = 0; i < size; i++) {
            long id = buf.readLong();
            this.suffixes.put(id, ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.suffixes.size());
        for (Long2ObjectMap.Entry<String> entry : this.suffixes.long2ObjectEntrySet()) {
            buf.writeLong(entry.getLongKey());
            ByteBufUtils.writeUTF8String(buf, entry.getValue());
        }
    }

    public static class Handler implements IMessageHandler<SPacketInterfaceSuffixUpdate, IMessage> {

        @Override
        public IMessage onMessage(SPacketInterfaceSuffixUpdate message, MessageContext ctx) {
            GuiScreen screen = Minecraft.getMinecraft().currentScreen;
            if (screen instanceof GuiWirelessDualInterfaceTerminal gui) {
                gui.applySearchSuffixes(message.suffixes);
            }
            return null;
        }
    }
}
