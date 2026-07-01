package com.asdflj.ae2thing.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import com.asdflj.ae2thing.client.gui.IGuiMonitorTerminal;

import appeng.api.storage.data.IAEFluidStack;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Dedicated packet for fluid updates.
 */
public class SPacketMEFluidInvUpdate extends SPacketMEBaseInvUpdate implements IMessage {

    public SPacketMEFluidInvUpdate() {}

    public void appendFluid(final IAEFluidStack is) {
        this.list.add(is);
    }

    public static class Handler implements IMessageHandler<SPacketMEFluidInvUpdate, IMessage> {

        @Override
        public IMessage onMessage(SPacketMEFluidInvUpdate message, MessageContext ctx) {
            final GuiScreen gs = Minecraft.getMinecraft().currentScreen;
            if (gs instanceof IGuiMonitorTerminal gpt) {
                gpt.postStackUpdate(message.list);
            }
            return null;
        }
    }
}
