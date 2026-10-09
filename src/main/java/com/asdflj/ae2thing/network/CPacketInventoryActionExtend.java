package com.asdflj.ae2thing.network;

import static appeng.api.networking.crafting.CraftingItemList.ACTIVE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.api.InventoryActionExtend;
import com.asdflj.ae2thing.api.WirelessObject;
import com.asdflj.ae2thing.client.gui.container.ContainerCraftingTerminal;
import com.asdflj.ae2thing.client.gui.container.ContainerInfusionPatternTerminal;
import com.asdflj.ae2thing.client.gui.container.ContainerPatternModifier;
import com.asdflj.ae2thing.client.gui.container.ContainerPatternValueAmount;
import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.client.gui.container.IPatternValueContainer;
import com.asdflj.ae2thing.inventory.InventoryHandler;
import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.inventory.item.WirelessTerminal;
import com.asdflj.ae2thing.loader.ItemAndBlockHolder;
import com.asdflj.ae2thing.util.BlockPos;
import com.asdflj.ae2thing.util.CPUCraftingPreview;
import com.asdflj.ae2thing.util.InvUtil;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.interfaces.IInventorySlotAware;
import appeng.core.localization.GuiText;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

public class CPacketInventoryActionExtend implements IMessage {

    private InventoryActionExtend action;
    private int slot;
    private long id;
    private IAEStack<?> stack;
    private boolean isEmpty;

    public CPacketInventoryActionExtend() {}

    public CPacketInventoryActionExtend(final InventoryActionExtend action, final int slot, final int id) {
        this(action, slot, id, null);
    }

    public CPacketInventoryActionExtend(final InventoryActionExtend action) {
        this(action, 0, 0, null);
    }

    public CPacketInventoryActionExtend(final InventoryActionExtend action, final int slot, final int id,
        IAEStack<?> stack) {
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
                throw new EncoderException("Failed to encode extended inventory action stack", e);
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = PacketDecodeUtil.readIntEnum(buf, InventoryActionExtend.values(), "extended inventory action");
        slot = buf.readInt();
        id = buf.readLong();
        isEmpty = buf.readBoolean();
        if (!isEmpty) {
            try {
                stack = IAEStack.fromPacketGeneric(buf);
            } catch (IOException e) {
                throw new DecoderException("Failed to decode extended inventory action stack", e);
            }
        }
    }

    public static class Handler implements IMessageHandler<CPacketInventoryActionExtend, IMessage> {

        private void extractItemFromME(EntityPlayer player, IAEItemStack requestItem, int slot) {
            if (requestItem.getStackSize() <= 0) {
                return;
            }
            List<ItemStack> items = InvUtil
                .matcher(player, stack -> stack != null && stack.getItem() instanceof IWirelessTermHandler);
            for (ItemStack item : items) {
                try {
                    WirelessObject object = new WirelessObject(item, player.worldObj, slot, 0, 0, player);
                    if (object.rangeCheck() && requestItem.getStackSize() > 0) {
                        IAEItemStack result = object.getItemInventory()
                            .extractItems(requestItem, Actionable.MODULATE, object.getSource());
                        if (result != null) {
                            requestItem.decStackSize(result.getStackSize());
                        }
                        if (requestItem.getStackSize() <= 0) {
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        @Nullable
        @Override
        public IMessage onMessage(CPacketInventoryActionExtend message, MessageContext ctx) {
            final EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            if (message.action == InventoryActionExtend.REQUEST_ITEM) {
                if (message.slot < 0 || message.slot >= sender.inventory.mainInventory.length
                    || !(message.stack instanceof IAEItemStack request)
                    || sender.inventory.mainInventory[message.slot] != null) {
                    return null;
                }
                request.setStackSize(request.getItemStack().getMaxStackSize());
                IAEItemStack requestItem = request.copy();
                extractItemFromME(sender,requestItem,message.slot);
                request.decStackSize(requestItem.getStackSize());
                if(request.getStackSize() > 0){
                    sender.inventory.setInventorySlotContents(message.slot,request.getItemStack());
                }
                return null;
            }
            if(sender.openContainer instanceof ContainerCraftingTerminal) {
                return null;
            }
            if (sender.openContainer instanceof final AEBaseContainer baseContainer) {
                Object target = baseContainer.getTarget();
                if (message.action == InventoryActionExtend.SET_PATTERN_NAME) {
                    final ContainerOpenContext context = baseContainer.getOpenContext();
                    if (context != null && message.stack != null) {
                        final ForgeDirection side = Objects.requireNonNull(context.getSide());
                        final TileEntity te = context.getTile();
                        final BlockPos pos;
                        if (te != null) {
                            pos = new BlockPos(te);
                            InventoryHandler
                                .openGui(sender, te.getWorldObj(), pos, side, GuiType.PATTERN_NAME_SET);
                        } else if (target instanceof WirelessTerminal wirelessTerminal) {
                            pos = new BlockPos(wirelessTerminal.getInventorySlot(), 0, 0);
                            InventoryHandler
                                .openGui(sender, sender.getEntityWorld(), pos, side, GuiType.PATTERN_NAME_SET_ITEM);
                        } else {
                            return null;
                        }
                        if (sender.openContainer instanceof final ContainerPatternValueAmount cpv) {
                            final StorageName storage;
                            final int index;
                            if (message.id >= 0 && message.id < StorageName.values().length) {
                                storage = StorageName.values()[(int) message.id];
                                index = message.slot;
                            } else {
                                final Slot clickedSlot = baseContainer.getSlot(message.slot);
                                storage = baseContainer instanceof IPatternValueContainer owner
                                    ? owner.getAEStorageName(clickedSlot)
                                    : null;
                                index = clickedSlot == null ? -1 : clickedSlot.getSlotIndex();
                            }
                            final GuiType originGui = getOriginGui(baseContainer);
                            if (storage == null || index < 0 || originGui == null) {
                                return null;
                            }
                            cpv.setPrimaryGui(
                                ContainerPatternValueAmount
                                    .primaryGui(originGui, getOriginGuiIcon(originGui), pos, side));
                            cpv.updateVirtualSlot(storage, index, message.stack);
                            cpv.detectAndSendChanges();
                        }
                    }
                } else if (message.action == InventoryActionExtend.GET_CRAFTING_STATE) {
                    if(target instanceof IActionHost gh){
                        if (gh.getActionableNode() == null || gh.getActionableNode().getGrid() == null) {
                            return null;
                        }
                        ICraftingGrid craftingGrid = gh.getActionableNode().getGrid().getCache(ICraftingGrid.class);
                        NBTTagCompound cpuData = new NBTTagCompound();
                        NBTTagList tagList = new NBTTagList();
                        cpuData.setTag(Constants.CPU_LIST,tagList);
                        int i = 0;
                        if (craftingGrid == null || message.stack == null) {
                            return null;
                        }
                        for (ICraftingCPU cpu: craftingGrid.getCpus()) {
                            i++;
                            if(cpu instanceof CraftingCPUCluster ccc && ccc.getFinalOutput() != null){
                                if(message.stack instanceof IAEItemStack requestStack
                                    && requestStack.isSameType(ccc.getFinalOutput())){
                                    IItemList<IAEItemStack> list =  AEApi.instance().storage().createPrimitiveItemList();
                                    ccc.getListOfItem(list,ACTIVE);
                                    List<IAEItemStack> activeItems = getActiveCraftingItems(list);
                                    if(activeItems.isEmpty()){
                                        continue;
                                    }
                                    NBTTagCompound data = new NBTTagCompound();
                                    final String name;
                                    if(ccc.getName().isEmpty()){
                                        name = GuiText.CPUs.getLocal() + ": #" + i;
                                    }else{
                                        name =GuiText.CPUs.getLocal() + ": "  + ccc.getName().substring(0, Math.min(20, ccc.getName().length()));
                                    }
                                    new CPUCraftingPreview(name, ccc.getRemainingItemCount(),ccc.getElapsedTime(),  activeItems).writeToNBT(data);
                                    tagList.appendTag(data);
                                }
                            }
                        }
                        AE2Thing.proxy.netHandler.sendTo(new SPacketCraftingStateUpdate(cpuData),ctx.getServerHandler().playerEntity);
                    }
                } else if (message.action == InventoryActionExtend.CLEAR_PATTERN && baseContainer instanceof ContainerPatternModifier patternModifier) {
                    patternModifier.clearPattern();
                } else if (message.action == InventoryActionExtend.REPLACE_PATTERN && baseContainer instanceof ContainerPatternModifier patternModifier) {
                    patternModifier.replacePattern();
                } else if (message.action == InventoryActionExtend.SET_PATTERN_VALUE) {
                    final ContainerOpenContext context = baseContainer.getOpenContext();
                    if (context != null && message.stack != null) {
                        final ForgeDirection side = Objects.requireNonNull(context.getSide());
                        final TileEntity te = context.getTile();
                        final BlockPos pos;
                        if (te != null) {
                            pos = new BlockPos(te);
                            InventoryHandler
                                .openGui(sender, te.getWorldObj(), pos, side, GuiType.PATTERN_VALUE_SET);
                        } else if (target instanceof IInventorySlotAware slotAware) {
                            pos = new BlockPos(slotAware.getInventorySlot(), 0, 0);
                            InventoryHandler
                                .openGui(sender, sender.getEntityWorld(), pos, side, GuiType.PATTERN_VALUE_SET_ITEM);
                        } else {
                            return null;
                        }
                        if (sender.openContainer instanceof final ContainerPatternValueAmount cpv) {
                            final StorageName storage;
                            final int index;
                            if (message.id >= 0 && message.id < StorageName.values().length) {
                                storage = StorageName.values()[(int) message.id];
                                index = message.slot;
                            } else {
                                final Slot clickedSlot = baseContainer.getSlot(message.slot);
                                storage = baseContainer instanceof IPatternValueContainer owner
                                    ? owner.getAEStorageName(clickedSlot)
                                    : null;
                                index = clickedSlot == null ? -1 : clickedSlot.getSlotIndex();
                            }
                            final GuiType originGui = getOriginGui(baseContainer);
                            if (storage == null || index < 0 || originGui == null) {
                                return null;
                            }
                            cpv.setPrimaryGui(
                                ContainerPatternValueAmount
                                    .primaryGui(originGui, getOriginGuiIcon(originGui), pos, side));
                            cpv.updateVirtualSlot(storage, index, message.stack);
                            cpv.detectAndSendChanges();
                        }
                    }
                }
            }
            return null;
        }

        private GuiType getOriginGui(Object container) {
            if (container instanceof ContainerInfusionPatternTerminal) {
                return GuiType.INFUSION_PATTERN_TERMINAL;
            }
            if (container instanceof ContainerPatternModifier) {
                return GuiType.PATTERN_MODIFIER;
            }
            if (container instanceof ContainerWirelessDualInterfaceTerminal) {
                return GuiType.WIRELESS_DUAL_INTERFACE_TERMINAL;
            }
            return null;
        }

        private ItemStack getOriginGuiIcon(GuiType guiType) {
            return switch (guiType) {
                case INFUSION_PATTERN_TERMINAL -> ItemAndBlockHolder.INFUSION_PATTERN_TERMINAL.stack();
                case PATTERN_MODIFIER -> ItemAndBlockHolder.ITEM_PATTERN_MODIFIER.stack();
                case WIRELESS_DUAL_INTERFACE_TERMINAL -> ItemAndBlockHolder.ITEM_WIRELESS_DUAL_INTERFACE_TERMINAL
                    .stack();
                default -> null;
            };
        }

        private List<IAEItemStack> getActiveCraftingItems(IItemList<IAEItemStack> list) {
            List<IAEItemStack> activeItems = new ArrayList<>();
            for (IAEItemStack item : list) {
                activeItems.add(item);
            }
            activeItems.sort(
                Comparator.comparingLong(IAEItemStack::getStackSize)
                    .reversed());
            if (activeItems.size() <= CPUCraftingPreview.maxSize) {
                return activeItems;
            }
            return new ArrayList<>(activeItems.subList(0, CPUCraftingPreview.maxSize));
        }
    }

}
