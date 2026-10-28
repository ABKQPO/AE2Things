package com.asdflj.ae2thing.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDualInterfaceTerminal;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class CPacketInterfaceRecipeMapRequest implements IMessage {

    public CPacketInterfaceRecipeMapRequest() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<CPacketInterfaceRecipeMapRequest, IMessage> {

        @Override
        public IMessage onMessage(CPacketInterfaceRecipeMapRequest message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player != null && player.openContainer instanceof ContainerWirelessDualInterfaceTerminal container) {
                container.resendInterfaceRecipeMaps();
            }
            return null;
        }
    }
}
