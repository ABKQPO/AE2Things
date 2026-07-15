package com.asdflj.ae2thing.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.client.gui.container.ContainerRenamer;
import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.inventory.InventoryHandler;
import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.inventory.item.IClickableInTerminal;
import com.asdflj.ae2thing.inventory.item.WirelessTerminal;
import com.asdflj.ae2thing.util.Ae2Reflect;
import com.asdflj.ae2thing.util.BlockPos;
import com.asdflj.ae2thing.util.Util;

import appeng.api.util.IInterfaceViewable;
import appeng.container.AEBaseContainer;
import appeng.helpers.ICustomNameObject;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.tile.networking.TileCableBus;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class CPacketRenamer implements IMessage {

    private static final int MAX_NAME_LENGTH = 128;

    private int x;
    private int y;
    private int z;
    private int dim;
    private ForgeDirection side;
    private Action action;
    private String text;

    public enum Action {
        OPEN,
        GET_TEXT,
        SET_TEXT,
    }

    public CPacketRenamer() {}

    public CPacketRenamer(String text) {
        this.action = Action.SET_TEXT;
        this.text = text;
    }

    public CPacketRenamer(Action a) {
        this.action = a;
    }

    public CPacketRenamer(int x, int y, int z, int dim, ForgeDirection side) {
        this.action = Action.OPEN;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dim = dim;
        this.side = side;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.action = PacketDecodeUtil.readIntEnum(buf, Action.values(), "renamer action");
        if (this.action == Action.OPEN) {
            this.x = buf.readInt();
            this.y = buf.readInt();
            this.z = buf.readInt();
            this.dim = buf.readInt();
            this.side = ForgeDirection.getOrientation(buf.readInt());
        } else if (this.action == Action.SET_TEXT) {
            this.text = PacketDecodeUtil.readUtf16(buf, MAX_NAME_LENGTH, "renamer text");
        }

    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.action.ordinal());
        if (this.action == Action.OPEN) {
            buf.writeInt(this.x);
            buf.writeInt(this.y);
            buf.writeInt(this.z);
            buf.writeInt(this.dim);
            buf.writeInt(side.ordinal());
        } else if (this.action == Action.SET_TEXT) {
            buf.writeInt(this.text.length());
            for (int i = 0; i < this.text.length(); i++) {
                buf.writeChar(this.text.charAt(i));
            }
        }
    }

    public static class Handler implements IMessageHandler<CPacketRenamer, IMessage> {

        private String getName(Object obj) {
            String name = "";
            if (obj instanceof ICustomNameObject cno && cno.hasCustomName()) {
                name = cno.getCustomName();
            }
            if (name.isEmpty() && obj instanceof IInterfaceViewable iv) {
                name = CraftingCPUCluster.translateFromNetwork(iv.getName());
            }
            return name;
        }

        private String getName(TileEntity tile, ForgeDirection side) {
            if (tile == null) return "";
            if (tile instanceof TileCableBus) {
                return getName(((TileCableBus) tile).getPart(side));
            } else {
                return getName(tile);
            }
        }

        private void setName(TileEntity tile, ForgeDirection side, String text) {
            if (tile == null || text == null || text.isEmpty()) return;
            Object target = tile instanceof TileCableBus cableBus ? cableBus.getPart(side) : tile;
            if (target instanceof ICustomNameObject customNameObject) {
                customNameObject.setCustomName(text);
            }
        }

        @Override
        public IMessage onMessage(CPacketRenamer message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (!(player.openContainer instanceof AEBaseContainer con)) return null;
            switch (message.action) {
                case OPEN -> {
                    if (con instanceof ContainerWirelessDualInterfaceTerminal terminalContainer
                        && con.getTarget() instanceof IClickableInTerminal clickableInterface) {
                        World world = DimensionManager.getWorld(message.dim);
                        if (world == null) break;
                        TileEntity tile = world.getTileEntity(message.x, message.y, message.z);
                        Object target = tile instanceof TileCableBus cableBus ? cableBus.getPart(message.side) : tile;
                        if (!(target instanceof ICustomNameObject)
                            || !Ae2Reflect.getTracked(terminalContainer.delegateContainer)
                                .containsKey(target)) {
                            break;
                        }

                        String name = getName(tile, message.side);
                        Util.DimensionalCoordSide renamingTarget = new Util.DimensionalCoordSide(
                            message.x,
                            message.y,
                            message.z,
                            message.dim,
                            message.side,
                            name);
                        clickableInterface.setClickedInterface(renamingTarget);

                        if (con.getTarget() instanceof WirelessTerminal terminal) {
                            InventoryHandler.openGui(
                                player,
                                player.worldObj,
                                new BlockPos(terminal.getInventorySlot(), 0, 0),
                                message.side,
                                GuiType.RENAMER);
                            if (player.openContainer instanceof ContainerRenamer renamer) {
                                renamer.setRenamingTarget(renamingTarget);
                            }
                        }
                    }
                }
                case GET_TEXT -> {
                    if (con instanceof ContainerRenamer renamer) {
                        Util.DimensionalCoordSide intMsg = renamer.getRenamingTarget();
                        if (intMsg == null) break;
                        World world = DimensionManager.getWorld(intMsg.getDimension());
                        if (world == null) break;
                        TileEntity tile = world.getTileEntity(intMsg.x, intMsg.y, intMsg.z);
                        AE2Thing.proxy.netHandler
                            .sendTo(new SPacketStringUpdate(this.getName(tile, intMsg.getSide())), player);
                    }
                }
                case SET_TEXT -> {
                    if (con instanceof ContainerRenamer renamer) {
                        Util.DimensionalCoordSide intMsg = renamer.getRenamingTarget();
                        if (intMsg == null) break;
                        World world = DimensionManager.getWorld(intMsg.getDimension());
                        if (world == null) break;
                        TileEntity tile = world.getTileEntity(intMsg.x, intMsg.y, intMsg.z);
                        this.setName(tile, intMsg.getSide(), message.text);
                        AE2Thing.proxy.netHandler.sendTo(new SPacketSwitchBack(), player);
                    }
                }
            }
            return null;
        }

    }
}
