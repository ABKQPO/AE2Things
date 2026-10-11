package com.asdflj.ae2thing.network;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import com.asdflj.ae2thing.client.gui.GuiWirelessDualInterfaceTerminal;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class SPacketInterfaceSuffixUpdate implements IMessage {

    private Map<Long, String> suffixes = new HashMap<>();

    @SuppressWarnings("unused")
    public SPacketInterfaceSuffixUpdate() {}

    public SPacketInterfaceSuffixUpdate(Map<Long, String> suffixes) {
        this.suffixes = suffixes;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        this.suffixes = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            long id = buf.readLong();
            this.suffixes.put(id, ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.suffixes.size());
        for (Map.Entry<Long, String> entry : this.suffixes.entrySet()) {
            buf.writeLong(entry.getKey());
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
