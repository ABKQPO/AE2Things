package com.asdflj.ae2thing.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

import com.asdflj.ae2thing.client.gui.container.ITypeFilterContainer;

import appeng.api.storage.data.AEStackTypeRegistry;
import appeng.api.storage.data.IAEStackType;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;

public class CPacketTypeFilter implements IMessage {

    private int windowId;
    private Reference2BooleanMap<IAEStackType<?>> map;

    public CPacketTypeFilter() {
        // NO-OP
    }

    public CPacketTypeFilter(Reference2BooleanMap<IAEStackType<?>> map, int windowId) {
        this.map = map;
        this.windowId = windowId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.map = new Reference2BooleanOpenHashMap<>();
        int registeredTypeCount = 0;
        for (IAEStackType<?> type : AEStackTypeRegistry.getAllTypes()) {
            this.map.put(type, true);
            registeredTypeCount++;
        }
        this.windowId = buf.readInt();
        final int size = buf.readInt();
        if (size < 0 || size > registeredTypeCount) {
            throw new DecoderException("Invalid terminal type-filter count: " + size);
        }
        for (int i = 0; i < size; i++) {
            final String typeId = ByteBufUtils.readUTF8String(buf);
            final boolean value = buf.readBoolean();
            final IAEStackType<?> type = AEStackTypeRegistry.getType(typeId);
            if (type != null) {
                this.map.put(type, value);
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.windowId);
        buf.writeInt(this.map.size());
        for (Reference2BooleanMap.Entry<IAEStackType<?>> entry : this.map.reference2BooleanEntrySet()) {
            ByteBufUtils.writeUTF8String(
                buf,
                entry.getKey()
                    .getId());
            buf.writeBoolean(entry.getBooleanValue());
        }
    }

    public static class Handler implements IMessageHandler<CPacketTypeFilter, IMessage> {

        @Override
        public IMessage onMessage(CPacketTypeFilter message, MessageContext ctx) {
            final EntityPlayer player = ctx.getServerHandler().playerEntity;
            final Container c = player.openContainer;
            if (message.windowId != -1 && message.windowId != c.windowId) {
                return null;
            }
            if (c instanceof ITypeFilterContainer container) {
                container.updateTypeFilters(message.map, player);
            }
            return null;
        }
    }
}
