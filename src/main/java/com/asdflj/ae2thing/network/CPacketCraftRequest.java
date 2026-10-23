package com.asdflj.ae2thing.network;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.ae2thing.inventory.InventoryHandler;
import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.inventory.item.WirelessTerminal;
import com.asdflj.ae2thing.util.BlockPos;

import appeng.api.config.CraftingMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.storage.data.IAEStack;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.core.AELog;
import appeng.me.cache.CraftingGridCache;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

public class CPacketCraftRequest implements IMessage {

    private static final int FLAG_SHIFT = 1;
    private static final int FLAG_CTRL = 2;
    private static final int FLAG_LITE = 4;
    private static final int FLAG_TARGET = 8;

    private long amount = 1;
    private boolean heldShift;
    private boolean heldCtrl;
    private boolean liteMode;
    private IAEStack<?> target;
    private Mode mode;
    private CraftingMode craftingMode = CraftingMode.STANDARD;

    private enum Mode {
        TARGET,
        CRAFT
    }

    @SuppressWarnings("unused")
    public CPacketCraftRequest() {}

    public CPacketCraftRequest(final IAEStack<?> target, final long amount) {
        this.mode = Mode.TARGET;
        this.amount = Math.max(1, amount);
        this.target = target == null ? null : target.copy();
    }

    public CPacketCraftRequest(final long craftAmt, final boolean shift, final boolean ctrl,
        final CraftingMode currentValue, final boolean liteMode) {
        this.mode = Mode.CRAFT;
        this.amount = craftAmt;
        this.heldShift = shift;
        this.heldCtrl = ctrl;
        this.craftingMode = currentValue == null ? CraftingMode.STANDARD : currentValue;
        this.liteMode = liteMode;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        int flags = 0;
        if (this.heldShift) flags |= FLAG_SHIFT;
        if (this.heldCtrl) flags |= FLAG_CTRL;
        if (this.liteMode) flags |= FLAG_LITE;
        if (this.target != null) flags |= FLAG_TARGET;

        buf.writeByte(this.mode.ordinal());
        buf.writeByte(this.craftingMode.ordinal());
        buf.writeByte(flags);
        buf.writeLong(amount);
        if (this.target != null) {
            try {
                IAEStack.writeToPacketGeneric(buf, target);
            } catch (Exception e) {
                throw new EncoderException("Failed to encode crafting request", e);
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.mode = PacketDecodeUtil.readByteEnum(buf, Mode.values(), "craft request mode");
        this.craftingMode = PacketDecodeUtil.readByteEnum(buf, CraftingMode.values(), "crafting mode");
        final int flags = buf.readUnsignedByte();
        this.heldShift = (flags & FLAG_SHIFT) != 0;
        this.heldCtrl = (flags & FLAG_CTRL) != 0;
        this.liteMode = (flags & FLAG_LITE) != 0;
        this.amount = buf.readLong();
        if ((flags & FLAG_TARGET) != 0) {
            try {
                this.target = IAEStack.fromPacketGeneric(buf);
            } catch (IOException | RuntimeException e) {
                throw new DecoderException("Failed to decode craft request target", e);
            }
        }
    }

    public static class Handler implements IMessageHandler<CPacketCraftRequest, IMessage> {

        @Nullable
        @Override
        public IMessage onMessage(CPacketCraftRequest message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (!(player.openContainer instanceof final ContainerCraftAmount c)) return null;
            switch (message.mode) {
                case TARGET -> {
                    if (message.target == null) return null;
                    c.setItemToCraft(message.target);
                    c.setInitialCraftAmount(Math.max(message.amount, 1));
                    c.detectAndSendChanges();
                }
                case CRAFT -> {
                    if (message.amount <= 0) return null;
                    if (!(c.getTarget() instanceof final IGridHost host)) return null;
                    final IGridNode node = host.getGridNode(ForgeDirection.UNKNOWN);
                    if (node == null) return null;
                    final IGrid grid = node.getGrid();
                    if (grid == null || c.getItemToCraft() == null) return null;

                    final IAEStack<?> toCraft = c.getItemToCraft();
                    final long pre = toCraft.getStackSize();
                    toCraft.setStackSize(message.amount);

                    Future<ICraftingJob> job = null;
                    try {
                        final ICraftingGrid craftingGrid = grid.getCache(ICraftingGrid.class);
                        job = (craftingGrid instanceof final CraftingGridCache cache) ?
                            cache.beginCraftingJob(c.getWorld(), c.getGrid(), c.getActionSrc(), toCraft, message.craftingMode, message.liteMode, null) : craftingGrid.beginCraftingJob(c.getWorld(), c.getGrid(), c.getActionSrc(), toCraft, null);

                        final ContainerOpenContext context = c.getOpenContext();
                        if (context == null) {
                            job.cancel(true);
                            return null;
                        }

                        final TileEntity te = context.getTile();
                        if (te != null) {
                            InventoryHandler.openGui(player, te.getWorldObj(), new BlockPos(te),
                                Objects.requireNonNull(context.getSide()), GuiType.CRAFTING_CONFIRM);
                        } else {
                            InventoryHandler.openGui(player, player.getEntityWorld(),
                                new BlockPos(((WirelessTerminal) c.getTarget()).getInventorySlot(), 0, 0),
                                Objects.requireNonNull(context.getSide()), GuiType.CRAFTING_CONFIRM_ITEM);
                        }

                        if (player.openContainer instanceof final ContainerCraftConfirm containerConfirm) {
                            containerConfirm.setItemToCraft(c.getItemToCraft());
                            containerConfirm.setAutoStart(message.heldShift && !message.heldCtrl);
                            containerConfirm.setAutoStartAndFollow(!message.heldShift && message.heldCtrl);
                            containerConfirm.setJob(job);
                            containerConfirm.detectAndSendChanges();
                        } else {
                            job.cancel(true);
                        }
                    } catch (final Throwable e) {
                        if (job != null) job.cancel(true);
                        toCraft.setStackSize(pre);
                        AELog.debug(e);
                    }
                }
            }

            return null;
        }
    }
}
