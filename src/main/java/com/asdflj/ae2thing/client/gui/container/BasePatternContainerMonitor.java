package com.asdflj.ae2thing.client.gui.container;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraftforge.common.util.ForgeDirection;

import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.inventory.IPatternTerminal;
import com.asdflj.ae2thing.inventory.item.INetworkTerminal;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.ITerminalHost;
import appeng.api.util.IConfigurableObject;
import appeng.container.sync.SyncRegistrar;
import appeng.container.sync.handlers.BooleanSyncHandler;
import appeng.container.sync.handlers.IntSyncHandler;
import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.IContainerCraftingPacket;
import appeng.me.helpers.ChannelPowerSrc;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.util.IConfigManagerHost;

public abstract class BasePatternContainerMonitor extends ContainerMonitor implements IConfigurableObject,
    IConfigManagerHost, IAEAppEngInventory, IContainerCraftingPacket, IPatternContainer {

    protected SlotRestrictedInput patternSlotIN;
    protected SlotRestrictedInput patternSlotOUT;
    protected SlotRestrictedInput patternRefiller;

    protected final IInventory crafting;
    protected final IInventory output;
    protected final IInventory patternInv;

    public boolean canAccessViewCells;
    public boolean combine = false;
    public int activePage = 0;
    public boolean craftingMode = true;
    private final BooleanSyncHandler canAccessViewCellsSync;
    private final BooleanSyncHandler combineSync;
    private final IntSyncHandler activePageSync;
    private final BooleanSyncHandler craftingModeSync;
    protected final IPatternTerminal it;

    public BasePatternContainerMonitor(InventoryPlayer ip, ITerminalHost monitorable) {
        super(ip, monitorable);
        final SyncRegistrar sync = this.syncRegistrar();
        this.canAccessViewCellsSync = sync.booleanS2C("canAccessViewCells").onClientChange((o, n) -> {
            this.canAccessViewCells = n;
            this.onUpdate("canAccessViewCells", o, n);
        });
        this.combineSync = sync.booleanS2C("combine").onClientChange((o, n) -> {
            this.combine = n;
            this.onUpdate("combine", o, n);
        });
        this.activePageSync = sync.intS2C("activePage").onClientChange((o, n) -> {
            this.activePage = n;
            this.onUpdate("activePage", o, n);
        });
        this.craftingModeSync = sync.booleanS2C("craftingMode").onClientChange((o, n) -> {
            this.craftingMode = n;
            this.onUpdate("craftingMode", o, n);
        });
        this.it = (IPatternTerminal) monitorable;
        this.canAccessViewCells = false;
        this.crafting = this.it.getInventoryByName(Constants.CRAFTING);
        this.output = this.it.getInventoryByName(Constants.OUTPUT);
        this.patternInv = this.it.getInventoryByName(Constants.PATTERN);
    }

    protected final void syncPatternState() {
        this.canAccessViewCellsSync.set(this.canAccessViewCells);
        this.combineSync.set(this.combine);
        this.activePageSync.set(this.activePage);
        this.craftingModeSync.set(this.craftingMode);
    }

    @Override
    void setMonitor() {
        if (this.host instanceof INetworkTerminal) {
            final IGridNode node = ((IGridHost) this.host).getGridNode(ForgeDirection.UNKNOWN);
            if (node != null) {
                this.networkNode = node;
                final IGrid g = node.getGrid();
                if (g != null) {
                    this.setPowerSource(new ChannelPowerSrc(this.networkNode, g.getCache(IEnergyGrid.class)));
                    IStorageGrid storageGrid = g.getCache(IStorageGrid.class);
                    this.monitor.setMonitor(storageGrid.getItemInventory());
                    this.fluidMonitor.setMonitor(storageGrid.getFluidInventory(), storageGrid.getItemInventory());
                    this.monitor.setFluidMonitorObject(this.fluidMonitor);
                    if (this.monitor.getMonitor() == null) {
                        this.setValidContainer(false);
                    } else {
                        this.monitor.addListener();
                        this.fluidMonitor.addListener();
                    }
                }
            } else {
                this.setValidContainer(false);
            }
        }
    }

    @Override
    public Slot getPatternOutputSlot() {
        return this.patternSlotOUT;
    }
}
