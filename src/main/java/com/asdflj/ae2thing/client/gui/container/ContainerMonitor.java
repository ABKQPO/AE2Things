package com.asdflj.ae2thing.client.gui.container;

import static appeng.util.IterationCounter.fetchNewId;
import static com.asdflj.ae2thing.api.Constants.MessageType.UPDATE_PLAYER_ITEM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import org.apache.commons.lang3.tuple.MutablePair;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.api.AE2ThingAPI;
import com.asdflj.ae2thing.client.gui.container.BaseMonitor.FluidMonitor;
import com.asdflj.ae2thing.client.gui.container.BaseMonitor.ItemMonitor;
import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.inventory.item.INetworkTerminal;
import com.asdflj.ae2thing.network.SPacketMEItemInvUpdate;
import com.asdflj.ae2thing.network.SPacketTypeFilter;
import com.asdflj.ae2thing.util.HBMAeAddonUtil;
import com.glodblock.github.common.item.ItemFluidPacket;
import com.glodblock.github.util.Util;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.ITerminalTypeFilterProvider;
import appeng.api.storage.data.AEStackTypeRegistry;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.core.AELog;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.IContainerCraftingPacket;
import appeng.helpers.MonitorableAction;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.util.ConfigManager;
import appeng.util.IConfigManagerHost;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.AdaptorPlayerHand;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEFluidStackType;
import appeng.util.item.AEItemStack;
import it.unimi.dsi.fastutil.objects.ObjectLongPair;

public abstract class ContainerMonitor extends BaseNetworkContainer implements IConfigurableObject, IConfigManagerHost,
    IAEAppEngInventory, IContainerCraftingPacket, ITypeFilterContainer {

    protected final IItemList<IAEItemStack> items = AEApi.instance()
        .storage()
        .createItemList();
    protected final IConfigManager clientCM;
    protected final ItemMonitor monitor;
    protected final FluidMonitor fluidMonitor;
    protected final List<GenericMonitor> genericMonitors = new ArrayList<>();
    protected ITerminalHost host;
    protected IConfigManagerHost gui;
    protected IConfigManager serverCM;
    protected IGridNode networkNode;
    private boolean typeFilterSynced = false;

    public ContainerMonitor(InventoryPlayer ip, ITerminalHost monitorable) {
        super(ip, monitorable);
        this.host = monitorable;
        this.clientCM = new ConfigManager(this);
        this.clientCM.registerSetting(Settings.SORT_BY, SortOrder.NAME);
        this.clientCM.registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
        this.clientCM.registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);
        this.monitor = new ItemMonitor(this.crafters);
        this.fluidMonitor = new FluidMonitor(this.crafters);
        if (Platform.isServer()) {
            if (monitorable instanceof INetworkTerminal) {
                this.networkNode = ((INetworkTerminal) monitorable).getGridNode();
            }
            this.serverCM = monitorable.getConfigManager();
            this.setMonitor();
        }
    }

    protected void dropItem(ItemStack is) {
        if (is == null || is.stackSize <= 0) return;
        ItemStack itemStack = is.copy();
        int i = itemStack.getMaxStackSize();
        while (itemStack.stackSize > 0) {
            if (i > itemStack.stackSize) {
                if (!getPlayerInv().addItemStackToInventory(itemStack.copy())) {
                    getPlayerInv().player.entityDropItem(itemStack.copy(), 0);
                }
                break;
            } else {
                itemStack.stackSize -= i;
                ItemStack item = itemStack.copy();
                item.stackSize = i;
                if (!getPlayerInv().addItemStackToInventory(item)) {
                    getPlayerInv().player.entityDropItem(item, 0);
                }
            }
        }
    }

    protected void dropItem(ItemStack itemStack, int stackSize) {
        if (itemStack == null || itemStack.stackSize <= 0) return;
        ItemStack is = itemStack.copy();
        is.stackSize = stackSize;
        this.dropItem(is);
    }

    protected void adjustStack(ItemStack stack) {
        if (stack != null && stack.stackSize > stack.getMaxStackSize()) {
            dropItem(stack, stack.stackSize - stack.getMaxStackSize());
            stack.stackSize = stack.getMaxStackSize();
        }
    }

    abstract void setMonitor();

    public void setGui(@Nonnull final IConfigManagerHost gui) {
        this.gui = gui;
    }

    public IMEMonitor<IAEItemStack> getMonitor() {
        return this.monitor.getMonitor();
    }

    public void doMonitorableAction(MonitorableAction action, EntityPlayerMP player) {
        IMEMonitor<IAEItemStack> itemMonitor = this.getMonitor();
        IAEItemStack slotItem = null;
        if (itemMonitor != null && this.getTargetStack() instanceof IAEItemStack target) {
            slotItem = itemMonitor.getAvailableItem(target, fetchNewId());
        }

        switch (action) {
            case SHIFT_CLICK -> {
                if (this.getPowerSource() == null || itemMonitor == null || slotItem == null) return;

                IAEItemStack toExtract = slotItem.copy();
                ItemStack item = toExtract.getItemStack();
                toExtract.setStackSize(item.getMaxStackSize());
                item.stackSize = (int) toExtract.getStackSize();

                InventoryAdaptor adaptor = InventoryAdaptor.getAdaptor(player, ForgeDirection.UNKNOWN);
                ItemStack remainder = adaptor.simulateAdd(item);
                if (remainder != null) {
                    toExtract.decStackSize(remainder.stackSize);
                }

                toExtract = Platform
                    .poweredExtraction(this.getPowerSource(), itemMonitor, toExtract, this.getActionSource());
                if (toExtract != null) {
                    adaptor.addItems(toExtract.getItemStack());
                }
            }
            case PICKUP_SINGLE, ROLL_UP -> {
                if (this.getPowerSource() == null || itemMonitor == null || slotItem == null) return;

                ItemStack hand = player.inventory.getItemStack();
                if (hand != null) {
                    if (hand.stackSize >= hand.getMaxStackSize()) return;
                    if (!Platform.isSameItemPrecise(slotItem.getItemStack(), hand)) return;
                }

                IAEItemStack toExtract = slotItem.copy();
                toExtract.setStackSize(1);
                toExtract = Platform
                    .poweredExtraction(this.getPowerSource(), itemMonitor, toExtract, this.getActionSource());
                if (toExtract != null) {
                    InventoryAdaptor handAdaptor = new AdaptorPlayerHand(player);
                    ItemStack remainder = handAdaptor.addItems(toExtract.getItemStack());
                    if (remainder != null) {
                        itemMonitor.injectItems(toExtract, Actionable.MODULATE, this.getActionSource());
                    }
                    this.updateHeld(player);
                }
            }
            case PICKUP_OR_SET_DOWN -> {
                if (this.getPowerSource() == null || itemMonitor == null) return;

                ItemStack hand = player.inventory.getItemStack();
                if (hand == null) {
                    if (slotItem == null) return;
                    this.pickupStoredItems(slotItem.copy(), player, itemMonitor);
                } else {
                    IAEItemStack toInsert = AEApi.instance()
                        .storage()
                        .createItemStack(hand);
                    toInsert = Platform
                        .poweredInsert(this.getPowerSource(), itemMonitor, toInsert, this.getActionSource());
                    player.inventory.setItemStack(toInsert == null ? null : toInsert.getItemStack());
                    this.updateHeld(player);
                }
            }
            case SPLIT_OR_PLACE_SINGLE -> {
                if (this.getPowerSource() == null || itemMonitor == null) return;

                ItemStack hand = player.inventory.getItemStack();
                if (hand == null) {
                    if (slotItem == null) return;
                    this.splitStoredItems(slotItem.copy(), player, itemMonitor);
                } else {
                    IAEItemStack toInsert = AEApi.instance()
                        .storage()
                        .createItemStack(hand);
                    toInsert.setStackSize(1);
                    toInsert = Platform
                        .poweredInsert(this.getPowerSource(), itemMonitor, toInsert, this.getActionSource());
                    if (toInsert == null) {
                        hand.stackSize--;
                        if (hand.stackSize <= 0) player.inventory.setItemStack(null);
                        this.updateHeld(player);
                    }
                }
            }
            case ROLL_DOWN -> {
                ItemStack hand = player.inventory.getItemStack();
                if (this.getPowerSource() == null || itemMonitor == null || hand == null) return;

                IAEItemStack toInsert = AEItemStack.create(hand);
                toInsert.setStackSize(1);
                toInsert = Platform.poweredInsert(this.getPowerSource(), itemMonitor, toInsert, this.getActionSource());
                if (toInsert == null) {
                    hand.stackSize--;
                    if (hand.stackSize <= 0) player.inventory.setItemStack(null);
                    this.updateHeld(player);
                }
            }
            case MOVE_REGION -> {
                if (this.getPowerSource() == null || itemMonitor == null || slotItem == null) return;

                long maxSize = slotItem.getItemStack()
                    .getMaxStackSize();
                InventoryAdaptor adaptor = InventoryAdaptor.getAdaptor(player, ForgeDirection.UNKNOWN);
                while (true) {
                    IAEItemStack toExtract = slotItem.copy();
                    toExtract.setStackSize(maxSize);
                    toExtract = itemMonitor.extractItems(toExtract, Actionable.SIMULATE, this.getActionSource());
                    if (toExtract == null || toExtract.getStackSize() <= 0) break;

                    ItemStack remainder = adaptor.simulateAdd(toExtract.getItemStack());
                    if (remainder != null) {
                        if (toExtract.getStackSize() == remainder.stackSize) break;
                        toExtract.decStackSize(remainder.stackSize);
                    }

                    toExtract = Platform
                        .poweredExtraction(this.getPowerSource(), itemMonitor, toExtract, this.getActionSource());
                    if (toExtract == null || toExtract.getStackSize() <= 0) break;
                    adaptor.addItems(toExtract.getItemStack());
                }
            }
            case CREATIVE_DUPLICATE -> {
                if (player.capabilities.isCreativeMode && slotItem != null) {
                    ItemStack item = slotItem.getItemStack();
                    item.stackSize = item.getMaxStackSize();
                    player.inventory.setItemStack(item);
                    this.updateHeld(player);
                }
            }
            case FILL_SINGLE_CONTAINER, FILL_CONTAINERS -> {
                if (this.getTargetStack() instanceof IAEFluidStack fluid) {
                    this.postChange(fluid, player, -1, action == MonitorableAction.FILL_CONTAINERS);
                } else if (this.getTargetStack() != null) {
                    this.processGenericContainer(
                        this.getTargetStack(),
                        player,
                        true,
                        action == MonitorableAction.FILL_CONTAINERS);
                }
            }
            case DRAIN_SINGLE_CONTAINER, DRAIN_CONTAINERS -> {
                ItemStack hand = player.inventory.getItemStack();
                IAEStackType<?> type = this.findContainerType(hand);
                if (type == null) return;
                if (type == AEFluidStackType.FLUID_STACK_TYPE) {
                    this.postChange(null, player, -1, action == MonitorableAction.DRAIN_CONTAINERS);
                } else {
                    this.processGenericContainer(null, player, false, action == MonitorableAction.DRAIN_CONTAINERS);
                }
            }
            default -> {}
        }
    }

    private IAEStackType<?> findContainerType(ItemStack stack) {
        if (stack == null) return null;
        for (IAEStackType<?> type : AEStackTypeRegistry.getAllTypes()) {
            if (type.isContainerItemForType(stack)) return type;
        }
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void processGenericContainer(IAEStack<?> requested, EntityPlayerMP player, boolean fill, boolean shift) {
        if (this.getPowerSource() == null) return;
        ItemStack hand = player.inventory.getItemStack();
        IAEStackType type = fill ? requested.getStackType() : this.findContainerType(hand);
        if (hand == null || type == null || !type.isContainerItemForType(hand)) return;
        IStorageGrid storage = this.networkNode == null || this.networkNode.getGrid() == null ? null
            : this.networkNode.getGrid()
                .getCache(IStorageGrid.class);
        IMEMonitor monitor = storage == null ? null : storage.getMEMonitor(type);
        if (monitor == null) return;
        int count = shift ? hand.stackSize : 1;
        for (int i = 0; i < count; i++) {
            hand = player.inventory.getItemStack();
            if (hand == null) break;
            if (fill) {
                IAEStack available = monitor
                    .extractItems(requested.copy(), Actionable.SIMULATE, this.getActionSource());
                if (available == null) break;
                ItemStack one = hand.copy();
                one.stackSize = 1;
                ObjectLongPair<ItemStack> result = type.fillContainer(one, available);
                if (result.left() == null || result.rightLong() <= 0) break;
                available.setStackSize(result.rightLong());
                if (Platform.poweredExtraction(this.getPowerSource(), monitor, available, this.getActionSource())
                    == null) break;
                if (hand.stackSize == 1) player.inventory.setItemStack(result.left());
                else {
                    Platform.addToPlayerInvOrDrop(player, result.left());
                    hand.stackSize--;
                }
            } else {
                IAEStack contained = type.getStackFromContainerItem(hand);
                if (contained == null) break;
                IAEStack leftover = Platform.poweredInsert(
                    this.getPowerSource(),
                    monitor,
                    contained.copy(),
                    this.getActionSource(),
                    Actionable.SIMULATE);
                long accepted = leftover == null ? contained.getStackSize()
                    : contained.getStackSize() - leftover.getStackSize();
                if (accepted <= 0) break;
                contained.setStackSize(accepted);
                ObjectLongPair<ItemStack> result = type.drainStackFromContainer(hand.copy(), contained);
                if (result.left() == null || result.rightLong() <= 0) break;
                contained.setStackSize(result.rightLong());
                if (Platform.poweredInsert(this.getPowerSource(), monitor, contained, this.getActionSource()) != null)
                    break;
                if (hand.stackSize == 1) player.inventory.setItemStack(result.left());
                else {
                    hand.stackSize--;
                    Platform.addToPlayerInvOrDrop(player, result.left());
                }
            }
        }
        this.updateHeld(player);
    }

    private void pickupStoredItems(IAEItemStack stack, EntityPlayerMP player, IMEMonitor<IAEItemStack> itemMonitor) {
        stack.setStackSize(
            stack.getItemStack()
                .getMaxStackSize());
        stack = Platform.poweredExtraction(this.getPowerSource(), itemMonitor, stack, this.getActionSource());
        player.inventory.setItemStack(stack == null ? null : stack.getItemStack());
        this.updateHeld(player);
    }

    private void splitStoredItems(IAEItemStack stack, EntityPlayerMP player, IMEMonitor<IAEItemStack> itemMonitor) {
        long maxSize = stack.getItemStack()
            .getMaxStackSize();
        stack.setStackSize(maxSize);
        stack = itemMonitor.extractItems(stack, Actionable.SIMULATE, this.getActionSource());
        if (stack != null) {
            long stackSize = Math.min(maxSize, stack.getStackSize());
            stack.setStackSize((stackSize + 1) >> 1);
            stack = Platform.poweredExtraction(this.getPowerSource(), itemMonitor, stack, this.getActionSource());
        }
        player.inventory.setItemStack(stack == null ? null : stack.getItemStack());
        this.updateHeld(player);
    }

    @Override
    public ITerminalTypeFilterProvider getTypeFilterHost() {
        return this.host instanceof ITerminalTypeFilterProvider provider ? provider : null;
    }

    protected boolean isInvalid() {
        return !this.monitor.isValid(null);
    }

    protected void processItemList() {
        this.monitor.processItemList();
        for (GenericMonitor generic : this.genericMonitors) generic.processItemList();
    }

    protected void registerGenericMonitor(IAEStackType<?> type) {
        if (this.networkNode == null || this.networkNode.getGrid() == null) return;
        IStorageGrid storage = this.networkNode.getGrid()
            .getCache(IStorageGrid.class);
        if (storage == null) return;
        IMEMonitor<?> monitor = storage.getMEMonitor(type);
        if (monitor == null) return;
        GenericMonitor generic = new GenericMonitor(monitor, this.crafters);
        genericMonitors.add(generic);
        ((IMEMonitor) monitor).addListener(generic, null);
    }

    protected static class GenericMonitor implements IMEMonitorHandlerReceiver<IAEStack> {

        private final IMEMonitor monitor;
        private final List<ICrafting> crafters;
        private final List<IAEStack<?>> changes = new ArrayList<>();

        public GenericMonitor(IMEMonitor monitor, List<ICrafting> crafters) {
            this.monitor = monitor;
            this.crafters = crafters;
        }

        @Override
        public boolean isValid(Object token) {
            return monitor != null;
        }

        @Override
        public void postChange(IBaseMonitor<IAEStack> monitor, Iterable<IAEStack> change, BaseActionSource source) {
            for (Object stack : change) if (stack instanceof IAEStack<?>ae) changes.add(ae.copy());
        }

        @Override
        public void onListUpdate() {}

        void processItemList() {
            if (changes.isEmpty()) return;
            SPacketMEItemInvUpdate packet = new SPacketMEItemInvUpdate();
            packet.addAll(changes);
            for (Object crafter : crafters)
                if (crafter instanceof EntityPlayerMP player) AE2Thing.proxy.netHandler.sendTo(packet, player);
            changes.clear();
        }

        void queueInventory(ICrafting crafter) {
            if (!(crafter instanceof EntityPlayerMP player)) return;
            SPacketMEItemInvUpdate packet = new SPacketMEItemInvUpdate();
            for (Object stack : monitor.getStorageList())
                if (stack instanceof IAEStack<?>ae) packet.appendStack(ae.copy());
            if (!packet.isEmpty()) AE2Thing.proxy.netHandler.sendTo(packet, player);
        }

        void removeListener() {
            monitor.removeListener(this);
        }
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer()) {
            if (isInvalid()) {
                this.setValidContainer(false);
            }
            if (this.serverCM != null) {
                for (final Settings set : this.serverCM.getSettings()) {
                    final Enum<?> sideLocal = this.serverCM.getSetting(set);
                    final Enum<?> sideRemote = this.clientCM.getSetting(set);

                    if (sideLocal != sideRemote) {
                        this.clientCM.putSetting(set, sideLocal);
                        for (final Object crafter : this.crafters) {
                            try {
                                NetworkHandler.instance.sendTo(
                                    new PacketValueConfig(set.name(), sideLocal.name()),
                                    (EntityPlayerMP) crafter);
                            } catch (final IOException e) {
                                AELog.debug(e);
                            }
                        }
                    }
                }
            }
            processItemList();
            syncTypeFilter();
            super.detectAndSendChanges();
        }
    }

    private void syncTypeFilter() {
        if (this.typeFilterSynced) {
            return;
        }
        final ITerminalTypeFilterProvider provider = this.getTypeFilterHost();
        if (provider == null) {
            this.typeFilterSynced = true;
            return;
        }
        for (final Object crafter : this.crafters) {
            if (crafter instanceof EntityPlayerMP playerMP) {
                AE2Thing.proxy.netHandler.sendTo(new SPacketTypeFilter(provider.getTypeFilter(playerMP)), playerMP);
            }
        }
        this.typeFilterSynced = true;
    }

    @Override
    public IConfigManager getConfigManager() {
        if (Platform.isServer()) {
            return this.serverCM;
        }
        return this.clientCM;
    }

    protected IConfigManagerHost getGui() {
        return this.gui;
    }

    @Override
    public void updateSetting(IConfigManager manager, Enum settingName, Enum newValue) {
        if (this.getGui() != null) {
            this.getGui()
                .updateSetting(manager, settingName, newValue);
        }
    }

    @Override
    public void addCraftingToCrafters(final ICrafting c) {
        super.addCraftingToCrafters(c);
        // A container can gain viewers after its initial sync; allow the type filter to reach them.
        this.typeFilterSynced = false;
        this.monitor.queueInventory(c);
    }

    @Override
    public void removeCraftingFromCrafters(final ICrafting c) {
        super.removeCraftingFromCrafters(c);
        this.monitor.removeCraftingFromCrafters(c);
    }

    @Override
    public void onContainerClosed(final EntityPlayer player) {
        super.onContainerClosed(player);
        if (this.monitor.getMonitor() != null) this.monitor.removeListener();
    }

    private void extractPlayerInventoryItemStack(EntityPlayer player, ItemStack itemStack, int stackSize) {
        for (int x = 0; x < player.inventory.mainInventory.length; x++) {
            ItemStack is = player.inventory.mainInventory[x];
            if (is == null) continue;
            if (Platform.isSameItemPrecise(is, itemStack)) {
                ItemStack tmp = is.copy();
                if (is.stackSize < stackSize) {
                    stackSize = is.stackSize;
                }
                is.stackSize -= stackSize;
                tmp.stackSize = stackSize;
                if (is.stackSize == 0) {
                    player.inventory.setInventorySlotContents(x, null);
                }
                player.inventory.setItemStack(tmp);
                player.inventory.markDirty();
                return;
            }
        }
    }

    private boolean canFillDefaultContainer(IAEFluidStack ifs) {
        if (ifs == null) return false;
        MutablePair<Integer, ItemStack> result = null;
        ItemStack container = AE2ThingAPI.instance()
            .getFluidContainer(ifs);
        if (Util.FluidUtil.isFluidContainer(
            AE2ThingAPI.instance()
                .getFluidContainer(ifs))) {
            result = Util.FluidUtil.fillStack(container, ifs.getFluidStack());
        }
        return result != null && result.left != 0;
    }

    public void postChange(IAEFluidStack fluid, EntityPlayer player, int slotIndex, boolean shift) {
        ItemStack targetStack = getTargetStack(player, slotIndex);
        if (targetStack == null) {
            if (!canFillDefaultContainer(fluid)) return;
            IAEItemStack extractItem = this.monitor.getMonitor()
                .extractItems(
                    AEItemStack.create(
                        AE2ThingAPI.instance()
                            .getFluidContainer(fluid)),
                    Actionable.MODULATE,
                    this.getActionSource());
            if (extractItem != null) {
                player.inventory.setItemStack(extractItem.getItemStack());
            } else {
                this.extractPlayerInventoryItemStack(
                    player,
                    AE2ThingAPI.instance()
                        .getFluidContainer(fluid),
                    1);
            }
            targetStack = getTargetStack(player, slotIndex);
        }

        if (targetStack == null) return;
        // The primary output itemstack
        if (fluid != null
            && ((Mods.HBM_AE_ADDON.isModLoaded() && HBMAeAddonUtil.getItemIsEmptyContainer(targetStack, fluid))
                || Util.FluidUtil.isEmpty(targetStack))) {
            // Situation 1.a: Empty fluid container, and nonnull slot
            extractFluid(fluid, player, slotIndex, shift);
        } else if ((Util.FluidUtil.isFluidContainer(targetStack) && !Util.FluidUtil.isEmpty(targetStack))
            || (Mods.HBM_AE_ADDON.isModLoaded() && HBMAeAddonUtil.getItemHasFluidType(targetStack))) {
                // Situation 2.a: We are holding a non-empty container.
                insertFluid(player, slotIndex, shift);
                // End of situation 2.a
            }
        // No op (Any other situation)

        this.detectAndSendChanges();
    }

    private ItemStack getTargetStack(EntityPlayer player, int slotIndex) {
        if (slotIndex == -1) {
            return player.inventory.getItemStack();
        } else {
            return player.inventory.getStackInSlot(slotIndex);
        }
    }

    /**
     * The insert operation. For input, we have a filled container stack. For outputs, we have the following:
     * <ol>
     * <li>Leftover filled container stack</li>
     * <li>Empty containers</li>
     * <li>Partially filled container x1</li>
     * </ol>
     * In order above, the itemstack at `slotIndex` is transformed into the output.
     */
    private void insertFluid(EntityPlayer player, int slotIndex, boolean shift) {
        ItemStack targetStack = getTargetStack(player, slotIndex);
        final int containersRequestedToInsert = shift ? targetStack.stackSize : 1;

        // Step 1: Determine container characteristics and verify fluid to be extractable
        final int fluidPerContainer;
        final FluidStack fluidStackPerContainer;
        final boolean partialInsertSupported;
        if (targetStack.getItem() instanceof IFluidContainerItem fcItem) {
            ItemStack test = targetStack.copy();
            test.stackSize = 1;
            fluidStackPerContainer = fcItem.drain(test, Integer.MAX_VALUE, false);
            if (fluidStackPerContainer == null || fluidStackPerContainer.amount == 0) {
                return;
            }

            fluidPerContainer = fluidStackPerContainer.amount;
            partialInsertSupported = true;
        } else if (FluidContainerRegistry.isContainer(targetStack)) {
            ItemStack emptyTank = FluidContainerRegistry.drainFluidContainer(targetStack);
            if (emptyTank == null) {
                return;
            }
            fluidStackPerContainer = FluidContainerRegistry.getFluidForFilledItem(targetStack);
            fluidPerContainer = fluidStackPerContainer.amount;
            partialInsertSupported = false;
        } else if (Mods.HBM_AE_ADDON.isModLoaded() && HBMAeAddonUtil.getItemHasFluidType(targetStack)) {
            ItemStack emptyTank = com.hbm.inventory.FluidContainerRegistry.getEmptyContainer(targetStack);
            if (emptyTank == null) {
                return;
            }
            fluidStackPerContainer = HBMAeAddonUtil.getFluidPerContainer(targetStack);
            fluidPerContainer = fluidStackPerContainer.amount;
            partialInsertSupported = false;
        } else {
            return;
        }

        // Step 2: determine network capacity
        final IAEFluidStack totalFluid = AEFluidStack.create(fluidStackPerContainer);
        totalFluid.setStackSize((long) fluidPerContainer * containersRequestedToInsert);

        final IAEFluidStack notInsertable = this.injectFluids(totalFluid, Actionable.SIMULATE);

        final long insertableFluid;
        if (notInsertable == null || notInsertable.getStackSize() == 0) {
            insertableFluid = totalFluid.getStackSize();
        } else {
            long insertable = totalFluid.getStackSize() - notInsertable.getStackSize();
            if (partialInsertSupported) {
                insertableFluid = insertable;
            } else {
                // avoid remainder
                insertableFluid = insertable - (insertable % fluidPerContainer);
            }
        }
        totalFluid.setStackSize(insertableFluid);

        // Step 3: perform insert
        final long totalInserted;
        final IAEFluidStack notInserted = this.injectFluids(totalFluid, Actionable.MODULATE);
        if (notInserted != null && notInserted.getStackSize() > 0) {
            // User has a setup that causes discrepancy between simulation and modulation. Likely double storage bus.
            long total = totalFluid.getStackSize() - notInserted.getStackSize();
            if (total == 0) {
                return;
            }
            if (partialInsertSupported) {
                totalInserted = total;
            } else {
                // We cant have partially filled containers -> user will receive a fluid packet as last resort
                long remainder = total % fluidPerContainer;
                if (remainder == 0) {
                    totalInserted = total;
                } else {
                    long overflowAmount = fluidPerContainer - remainder;
                    IAEFluidStack overflow = AEFluidStack.create(fluidStackPerContainer);
                    overflow.setStackSize(overflowAmount);
                    dropItem(ItemFluidPacket.newStack(overflow));
                    totalInserted = total + overflowAmount;
                }
            }
        } else {
            totalInserted = totalFluid.getStackSize();
        }

        // Step 4: calculate outputs
        final int emptiedTanks = (int) (totalInserted / fluidPerContainer);
        final int partialDrain = (int) (totalInserted % fluidPerContainer);
        final int partialTanks = partialDrain > 0 && partialInsertSupported ? 1 : 0;
        final int usedTanks = emptiedTanks + partialTanks;
        final int untouchedTanks = targetStack.stackSize - usedTanks;

        ItemStack emptiedTanksStack;
        final ItemStack partialTanksStack;

        if (targetStack.getItem() instanceof IFluidContainerItem fcItem) {
            if (emptiedTanks > 0) {
                emptiedTanksStack = targetStack.copy();
                emptiedTanksStack.stackSize = 1;
                fcItem.drain(emptiedTanksStack, fluidPerContainer, true);
                emptiedTanksStack.stackSize = emptiedTanks;
            } else {
                emptiedTanksStack = null;
            }
            if (partialTanks > 0) {
                partialTanksStack = targetStack.copy();
                partialTanksStack.stackSize = 1;
                fcItem.drain(partialTanksStack, partialDrain, true);
            } else {
                partialTanksStack = null;
            }
        } else if (Mods.HBM_AE_ADDON.isModLoaded() && HBMAeAddonUtil.getItemHasFluidType(targetStack)) {
            if (emptiedTanks > 0) {
                emptiedTanksStack = com.hbm.inventory.FluidContainerRegistry.getEmptyContainer(targetStack);
                emptiedTanksStack.stackSize = emptiedTanks;
            } else {
                emptiedTanksStack = null;
            }
            // Not possible > see Step 2 and Step 3
            partialTanksStack = null;
        } else {
            if (emptiedTanks > 0) {
                emptiedTanksStack = FluidContainerRegistry.drainFluidContainer(targetStack);
                emptiedTanksStack.stackSize = emptiedTanks;
            } else {
                emptiedTanksStack = null;
            }
            // Not possible > see Step 2 and Step 3
            partialTanksStack = null;
        }

        // Done. Put the output in the inventory or ground, and update stack size.
        boolean shouldSendStack = true;
        if (slotIndex == -1) {
            // Item is in mouse hand
            if (untouchedTanks > 0) {
                targetStack.stackSize = untouchedTanks;
                adjustStack(targetStack);
                dropItem(emptiedTanksStack);
                dropItem(partialTanksStack);
            } else if (emptiedTanksStack != null) {
                adjustStack(emptiedTanksStack);
                player.inventory.setItemStack(emptiedTanksStack);
                dropItem(partialTanksStack);
            } else if (partialTanksStack != null) {
                player.inventory.setItemStack(partialTanksStack);
            } else {
                player.inventory.setItemStack(null);
                shouldSendStack = false;
            }
        } else {
            // Shift clicked in
            if (untouchedTanks > 0) {
                targetStack.stackSize = untouchedTanks;
                adjustStack(targetStack);
                dropItem(emptiedTanksStack);
                dropItem(partialTanksStack);
            } else if (emptiedTanksStack != null) {
                adjustStack(emptiedTanksStack);
                player.inventory.setInventorySlotContents(slotIndex, emptiedTanksStack);
                dropItem(partialTanksStack);
            } else if (partialTanksStack != null) {
                player.inventory.setInventorySlotContents(slotIndex, partialTanksStack);
            } else {
                player.inventory.setInventorySlotContents(slotIndex, null);
                shouldSendStack = false;
            }
        }
        SPacketMEItemInvUpdate packet = new SPacketMEItemInvUpdate(UPDATE_PLAYER_ITEM);
        if (shouldSendStack) {
            packet.appendItem(
                AEApi.instance()
                    .storage()
                    .createItemStack(player.inventory.getItemStack()));
        }
        AE2Thing.proxy.netHandler.sendTo(packet, (EntityPlayerMP) player);
    }

    /**
     * The extract operation. For input, we have an empty container stack. For outputs, we have the following:
     * <ol>
     * <li>Leftover empty container stack</li>
     * <li>Filled containers (full)</li>
     * <li>Partially filled container x1</li>
     * </ol>
     * In order above, the itemstack at `slotIndex` is transformed into the output.
     */
    private void extractFluid(IAEFluidStack clientRequestedFluid, EntityPlayer player, int slotIndex, boolean shift) {
        if (slotIndex != -1) {
            // shift-click from inventory cant fill fluids
            return;
        }
        final ItemStack targetStack = player.inventory.getItemStack();
        final int containersRequestedToExtract = shift ? targetStack.stackSize : 1;

        final FluidStack clientRequestedFluidStack = clientRequestedFluid.getFluidStack();
        clientRequestedFluidStack.amount = Integer.MAX_VALUE;

        // Step 1: Determine container characteristics and verify fluid to be insertable
        final int fluidPerContainer;
        final boolean partialInsertSupported;
        if (targetStack.getItem() instanceof IFluidContainerItem fcItem) {
            ItemStack testStack = targetStack.copy();
            testStack.stackSize = 1;
            fluidPerContainer = fcItem.fill(testStack, clientRequestedFluidStack, false);
            if (fluidPerContainer == 0) {
                return;
            }
            partialInsertSupported = true;
        } else if (FluidContainerRegistry.isContainer(targetStack)) {
            fluidPerContainer = FluidContainerRegistry.getContainerCapacity(clientRequestedFluidStack, targetStack);
            partialInsertSupported = false;
        } else if (Mods.HBM_AE_ADDON.isModLoaded()
            && HBMAeAddonUtil.getItemIsEmptyContainer(targetStack, clientRequestedFluid)) {
                fluidPerContainer = HBMAeAddonUtil.getEmptyContainerAmount(targetStack, clientRequestedFluid);
                partialInsertSupported = false;
            } else {
                return;
            }

        // Step 2: determine fluid in network
        final IAEFluidStack totalRequestedFluid = clientRequestedFluid.copy();
        totalRequestedFluid.setStackSize((long) fluidPerContainer * containersRequestedToExtract);

        final IAEFluidStack availableFluid = this.extractFluids(totalRequestedFluid, Actionable.SIMULATE);
        if (availableFluid == null || availableFluid.getStackSize() == 0) {
            return;
        }

        if (availableFluid.getStackSize() != totalRequestedFluid.getStackSize() && !partialInsertSupported) {
            availableFluid.decStackSize(availableFluid.getStackSize() % fluidPerContainer);
        }

        // Step 3: perform extract
        final IAEFluidStack extracted = this.extractFluids(availableFluid, Actionable.MODULATE);
        final long totalExtracted = extracted != null ? extracted.getStackSize() : 0;

        // Step 4: calculate outputs
        final int filledTanks = (int) (totalExtracted / fluidPerContainer);
        final int partialFill = (int) (totalExtracted % fluidPerContainer);
        final int partialTanks = partialFill > 0 && partialInsertSupported ? 1 : 0;
        final int usedTanks = filledTanks + partialTanks;
        final int untouchedTanks = targetStack.stackSize - usedTanks;

        ItemStack filledTanksStack;
        ItemStack partialTanksStack;

        if (targetStack.getItem() instanceof IFluidContainerItem fcItem) {
            if (filledTanks > 0) {
                filledTanksStack = targetStack.copy();
                filledTanksStack.stackSize = 1;
                FluidStack toInsert = extracted.getFluidStack()
                    .copy();
                toInsert.amount = fluidPerContainer;
                fcItem.fill(filledTanksStack, toInsert, true);
                filledTanksStack.stackSize = filledTanks;
            } else {
                filledTanksStack = null;
            }
            if (partialTanks > 0) {
                partialTanksStack = targetStack.copy();
                partialTanksStack.stackSize = 1;
                FluidStack toInsert = extracted.getFluidStack()
                    .copy();
                toInsert.amount = partialFill;
                fcItem.fill(partialTanksStack, toInsert, true);
            } else {
                partialTanksStack = null;
            }
        } else if (Mods.HBM_AE_ADDON.isModLoaded()
            && HBMAeAddonUtil.getItemIsEmptyContainer(targetStack, clientRequestedFluid)) {
                if (filledTanks > 0) {
                    filledTanksStack = targetStack.copy();
                    filledTanksStack.stackSize = 1;
                    FluidStack toInsert = extracted.getFluidStack()
                        .copy();
                    toInsert.amount = fluidPerContainer;
                    filledTanksStack = HBMAeAddonUtil.getFillContainer(targetStack, clientRequestedFluid);
                    filledTanksStack.stackSize = filledTanks;
                } else {
                    filledTanksStack = null;
                }
                if (partialTanks > 0) {
                    partialTanksStack = targetStack.copy();
                    partialTanksStack.stackSize = 1;
                    FluidStack toInsert = extracted.getFluidStack()
                        .copy();
                    toInsert.amount = partialFill;
                    partialTanksStack = HBMAeAddonUtil.getFillContainer(targetStack, clientRequestedFluid);
                } else {
                    partialTanksStack = null;
                }
            } else {
                if (filledTanks > 0) {
                    FluidStack toInsert = extracted.getFluidStack()
                        .copy();
                    toInsert.amount = fluidPerContainer;
                    filledTanksStack = FluidContainerRegistry.fillFluidContainer(toInsert, targetStack);
                    filledTanksStack.stackSize = filledTanks;
                } else {
                    filledTanksStack = null;
                }
                if (partialFill > 0) {
                    // User has a setup that causes discrepancy between simulation and modulation. Likely double storage
                    // bus.
                    // We cant have partially filled containers -> user will receive a fluid packet as last resort
                    IAEFluidStack overflow = extracted.copy();
                    overflow.setStackSize(partialFill);
                    dropItem(ItemFluidPacket.newStack(overflow));
                }
                partialTanksStack = null;
            }

        // Done. Put the output in the inventory or ground, and update stack size.
        // We can assume slotIndex == -1, since we don't actually allow extraction via shift click.
        boolean shouldSendStack = true;
        if (untouchedTanks > 0) {
            ItemStack emptyStack = player.inventory.getItemStack();
            emptyStack.stackSize = untouchedTanks;
            adjustStack(emptyStack);
            dropItem(filledTanksStack);
            dropItem(partialTanksStack);
        } else if (filledTanksStack != null) {
            adjustStack(filledTanksStack);
            player.inventory.setItemStack(filledTanksStack);
            dropItem(partialTanksStack);
        } else if (partialTanksStack != null) {
            player.inventory.setItemStack(partialTanksStack);
        } else {
            player.inventory.setItemStack(null);
            shouldSendStack = false;
        }
        SPacketMEItemInvUpdate packet = new SPacketMEItemInvUpdate(UPDATE_PLAYER_ITEM);
        if (shouldSendStack) {
            packet.appendItem(
                AEApi.instance()
                    .storage()
                    .createItemStack(player.inventory.getItemStack()));
        }
        AE2Thing.proxy.netHandler.sendTo(packet, (EntityPlayerMP) player);
    }

    protected IAEFluidStack extractFluids(IAEFluidStack ifs, Actionable mode) {
        if (ifs.getStackSize() == 0) return ifs;
        return this.host.getFluidInventory()
            .extractItems(ifs, mode, this.getActionSource());

    }

    protected IAEFluidStack injectFluids(IAEFluidStack ifs, Actionable mode) {
        return this.host.getFluidInventory()
            .injectItems(ifs, mode, this.getActionSource());
    }
}
