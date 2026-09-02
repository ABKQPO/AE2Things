package com.asdflj.ae2thing.network;

import static com.asdflj.ae2thing.api.Constants.DISPLAY_ONLY;

import java.io.IOException;
import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.asdflj.ae2thing.inventory.InventoryHandler;
import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.inventory.item.WirelessTerminal;
import com.asdflj.ae2thing.util.BlockPos;
import com.glodblock.github.common.item.ItemFluidDrop;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.helpers.InventoryAction;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

public class CPacketInventoryAction implements IMessage {

    private InventoryAction action;
    private int slot;
    private long id;
    private IAEStack<?> stack;
    private boolean isEmpty;

    public CPacketInventoryAction() {}

    public CPacketInventoryAction(final InventoryAction action, final int slot, final int id) {
        this.action = action;
        this.slot = slot;
        this.id = id;
        this.stack = null;
        this.isEmpty = true;
    }

    public CPacketInventoryAction(final InventoryAction action, final int slot, final int id, IAEStack<?> stack) {
        this.action = action;
        this.slot = slot;
        this.id = id;
        this.stack = stack;
        this.isEmpty = stack == null;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action.ordinal());
        buf.writeInt(slot);
        buf.writeLong(id);
        buf.writeBoolean(isEmpty);
        if (!isEmpty) {
            try {
                IAEStack.writeToPacketGeneric(buf, stack);
            } catch (IOException e) {
                throw new EncoderException("Failed to encode inventory action stack", e);
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = PacketDecodeUtil.readIntEnum(buf, InventoryAction.values(), "inventory action");
        slot = buf.readInt();
        id = buf.readLong();
        isEmpty = buf.readBoolean();
        if (!isEmpty) {
            try {
                stack = IAEStack.fromPacketGeneric(buf);
            } catch (IOException e) {
                throw new DecoderException("Failed to decode inventory action stack", e);
            }
        }
    }

    public static class Handler implements IMessageHandler<CPacketInventoryAction, IMessage> {

        @Nullable
        @Override
        public IMessage onMessage(CPacketInventoryAction message, MessageContext ctx) {
            final EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            if (sender.openContainer instanceof final AEBaseContainer baseContainer) {
                Object target = baseContainer.getTarget();
                if (message.action == InventoryAction.AUTO_CRAFT) {
                    final ContainerOpenContext context = baseContainer.getOpenContext();
                    if (context != null) {
                        final TileEntity te = context.getTile();
                        if (te != null || target instanceof WirelessTerminal) {
                            if (message.stack == null){
                                message.stack = baseContainer.getTargetStack();
                            }
                            if(message.stack instanceof IAEItemStack itemStack
                                && itemStack.getItem() instanceof ItemFluidDrop){
                                ItemStack is = itemStack.getItemStack().copy();
                                NBTTagCompound data = is.getTagCompound();
                                if (data != null) {
                                    data.removeTag(DISPLAY_ONLY);
                                    is.setTagCompound(data);
                                }
                                baseContainer.setTargetStack(AEItemStack.create(is));
                            }else{
                                baseContainer.setTargetStack(message.stack);
                            }
                            if(te != null){
                                InventoryHandler.openGui(
                                    sender,
                                    te.getWorldObj(),
                                    new BlockPos(te),
                                    Objects.requireNonNull(baseContainer.getOpenContext().getSide()),
                                    GuiType.CRAFTING_AMOUNT);
                            }else{
                                InventoryHandler.openGui(
                                    sender,
                                    sender.getEntityWorld(),
                                    new BlockPos(((WirelessTerminal) target).getInventorySlot(),0,0),
                                    Objects.requireNonNull(baseContainer.getOpenContext().getSide()),
                                    GuiType.CRAFTING_AMOUNT_ITEM);
                            }
                        }
                        if (sender.openContainer instanceof final ContainerCraftAmount cca) {
                            if (baseContainer.getTargetStack() != null) {
                                cca.setItemToCraft(baseContainer.getTargetStack());
                            }
                            cca.detectAndSendChanges();
                        }
                    }
                } else {
                    baseContainer.doAction(sender, message.action, message.slot, message.id);
                }
            }
            return null;
        }
    }
}
