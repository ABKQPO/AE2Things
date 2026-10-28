package com.asdflj.ae2thing.network;

import com.asdflj.ae2thing.client.gui.container.ContainerUniversalStorageBus;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class CPacketUniversalStorageBusAction implements IMessage {

    private Action action;

    @SuppressWarnings("unused")
    public CPacketUniversalStorageBusAction() {

    }

    public CPacketUniversalStorageBusAction(Action action) {
        this.action = action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.action = PacketDecodeUtil.readByteEnum(buf, Action.values(), "universal storage bus action");
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(this.action.ordinal());
    }

    public static class Handler implements IMessageHandler<CPacketUniversalStorageBusAction, IMessage> {

        @Override
        public IMessage onMessage(CPacketUniversalStorageBusAction message, MessageContext ctx) {
            if (!(ctx.getServerHandler().playerEntity.openContainer instanceof ContainerUniversalStorageBus c))
                return null;
            switch (message.action) {
                case PARTITION -> c.partition(false);
                case PARTITION_CLEAR -> c.partition(true);
                case CLEAR -> c.clear();
            }
            return null;
        }
    }

    public enum Action {
        PARTITION,
        PARTITION_CLEAR,
        CLEAR
    }
}
