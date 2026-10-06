package com.asdflj.ae2thing.network;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;

import com.asdflj.ae2thing.client.gui.widget.IFlowRateGui;

import appeng.api.storage.data.IAEStack;
import appeng.me.cache.ItemFlowGridCache.FlowRate;
import appeng.util.Platform;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Delivers per-stack item flow rates to the open terminal.
 *
 * <p>
 * AE2 has an equivalent packet, but its client handler tests {@code instanceof GuiMEMonitorable}. Terminals that do not
 * extend that class can never receive it, so this mirrors the same wire format over AE2Things' own channel and delivers
 * through {@link IFlowRateGui} instead.
 */
public class SPacketFlowRates implements IMessage {

    private Map<IAEStack<?>, FlowRate> rates = new HashMap<>();

    public SPacketFlowRates() {
        // NO-OP
    }

    public SPacketFlowRates(Map<IAEStack<?>, FlowRate> rates) {
        this.rates = rates;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        final int size = buf.readInt();
        if (size < 0) {
            throw new IllegalArgumentException("Invalid flow-rate entry count: " + size);
        }
        this.rates = new HashMap<>();
        for (int i = 0; i < size; i++) {
            final IAEStack<?> stack = Platform.readStackByte(buf);
            final long in = buf.readLong();
            final long out = buf.readLong();
            if (stack != null) {
                this.rates.put(stack, new FlowRate(in, out));
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.rates.size());
        for (final Map.Entry<IAEStack<?>, FlowRate> entry : this.rates.entrySet()) {
            Platform.writeStackByte(entry.getKey(), buf);
            buf.writeLong(
                entry.getValue()
                    .in());
            buf.writeLong(
                entry.getValue()
                    .out());
        }
    }

    public static class Handler implements IMessageHandler<SPacketFlowRates, IMessage> {

        @Override
        public IMessage onMessage(SPacketFlowRates message, MessageContext ctx) {
            if (Minecraft.getMinecraft().currentScreen instanceof IFlowRateGui gui) {
                gui.updateFlowRates(message.rates);
            }
            return null;
        }
    }
}
