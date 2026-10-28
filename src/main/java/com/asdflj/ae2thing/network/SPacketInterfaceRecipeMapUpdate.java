package com.asdflj.ae2thing.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import com.asdflj.ae2thing.client.gui.GuiWirelessDualInterfaceTerminal;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class SPacketInterfaceRecipeMapUpdate implements IMessage {

    private Long2ObjectMap<String> recipeMaps = new Long2ObjectOpenHashMap<>();

    @SuppressWarnings("unused")
    public SPacketInterfaceRecipeMapUpdate() {}

    public SPacketInterfaceRecipeMapUpdate(Long2ObjectMap<String> recipeMaps) {
        this.recipeMaps = recipeMaps;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        final int size = buf.readInt();
        this.recipeMaps = new Long2ObjectOpenHashMap<>(size);
        for (int i = 0; i < size; i++) {
            this.recipeMaps.put(buf.readLong(), ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.recipeMaps.size());
        for (Long2ObjectMap.Entry<String> entry : this.recipeMaps.long2ObjectEntrySet()) {
            buf.writeLong(entry.getLongKey());
            ByteBufUtils.writeUTF8String(buf, entry.getValue());
        }
    }

    public static class Handler implements IMessageHandler<SPacketInterfaceRecipeMapUpdate, IMessage> {

        @Override
        public IMessage onMessage(SPacketInterfaceRecipeMapUpdate message, MessageContext ctx) {
            final GuiScreen screen = Minecraft.getMinecraft().currentScreen;
            if (screen instanceof GuiWirelessDualInterfaceTerminal gui) {
                gui.applyInterfaceRecipeMaps(message.recipeMaps);
            }
            return null;
        }
    }
}
