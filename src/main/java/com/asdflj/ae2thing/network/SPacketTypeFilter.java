package com.asdflj.ae2thing.network;

import net.minecraft.client.Minecraft;

import com.asdflj.ae2thing.client.gui.widget.ITypeFilterGui;

import appeng.api.storage.data.AEStackTypeRegistry;
import appeng.api.storage.data.IAEStackType;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;

public class SPacketTypeFilter implements IMessage {

    private Reference2BooleanMap<IAEStackType<?>> map;

    public SPacketTypeFilter() {
        // NO-OP
    }

    public SPacketTypeFilter(Reference2BooleanMap<IAEStackType<?>> map) {
        this.map = map;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.map = new Reference2BooleanOpenHashMap<>();
        for (IAEStackType<?> type : AEStackTypeRegistry.getAllTypes()) {
            this.map.put(type, true);
        }
        final int size = buf.readInt();
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
        buf.writeInt(this.map.size());
        for (Reference2BooleanMap.Entry<IAEStackType<?>> entry : this.map.reference2BooleanEntrySet()) {
            ByteBufUtils.writeUTF8String(
                buf,
                entry.getKey()
                    .getId());
            buf.writeBoolean(entry.getBooleanValue());
        }
    }

    public static class Handler implements IMessageHandler<SPacketTypeFilter, IMessage> {

        @Override
        public IMessage onMessage(SPacketTypeFilter message, MessageContext ctx) {
            if (Minecraft.getMinecraft().currentScreen instanceof ITypeFilterGui gui) {
                gui.updateTypeFilters(message.map);
            }
            return null;
        }
    }
}
