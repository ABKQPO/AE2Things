package com.asdflj.ae2thing.common.tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.asdflj.ae2thing.common.item.ItemPhial;
import com.asdflj.ae2thing.util.AspectUtil;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.events.MENetworkStorageEvent;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.ICellContainer;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.helpers.IPriorityHost;
import appeng.me.GridAccessException;
import appeng.me.storage.MEInventoryHandler;
import appeng.tile.grid.AENetworkTile;
import appeng.util.IterationCounter;
import appeng.util.item.AEItemStackType;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.common.storage.AEEssentiaStack;

public class TileEssentiaDiscretizer extends AENetworkTile implements IPriorityHost, ICellContainer {

    private final BaseActionSource ownActionSource = new MachineSource(this);
    private final PhialDiscretizingInventory phialInv = new PhialDiscretizingInventory();
    private boolean prevActiveState = false;

    public TileEssentiaDiscretizer() {
        super();
        getProxy().setIdlePowerUsage(3D);
        getProxy().setFlags(GridFlags.REQUIRE_CHANNEL);
    }

    @Override
    public boolean canBeRotated() {
        return false;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<IMEInventoryHandler> getCellArray(StorageChannel channel) {
        if (getProxy().isActive() && channel == StorageChannel.ITEMS) {
            return Collections.singletonList(phialInv.invHandler);
        }
        return Collections.emptyList();
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void setPriority(int newValue) {
        // do nothing
    }

    @Override
    public void blinkCell(int slot) {
        // do nothing
    }

    @Override
    public void gridChanged() {
        IMEMonitor<AEEssentiaStack> essentiaGrid = getEssentiaGrid();
        if (essentiaGrid != null) {
            essentiaGrid.addListener(phialInv, essentiaGrid);
        }
    }

    @Override
    public void saveChanges(IMEInventory cellInventory) {
        markDirty();
    }

    private IMEMonitor<AEEssentiaStack> getEssentiaGrid() {
        try {
            return AspectUtil.getEssentiaMonitor(getProxy().getGrid());
        } catch (GridAccessException e) {
            return null;
        }
    }

    private void updateState() {
        boolean isActive = getProxy().isActive();
        if (isActive != prevActiveState) {
            prevActiveState = isActive;
            try {
                getProxy().getGrid()
                    .postEvent(new MENetworkCellArrayUpdate());
            } catch (GridAccessException e) {
                // NO-OP
            }
        }
    }

    @MENetworkEventSubscribe
    public void onPowerUpdate(MENetworkPowerStatusChange event) {
        updateState();
    }

    @MENetworkEventSubscribe
    public void onChannelUpdate(MENetworkChannelsChanged event) {
        updateState();
    }

    @MENetworkEventSubscribe
    public void onStorageUpdate(MENetworkStorageEvent event) {
        updateState();
    }

    /**
     * Presents the network's essentia (held in the essentia channel) as phial items in the item channel, and converts
     * phial items injected here back into network essentia.
     */
    private class PhialDiscretizingInventory
        implements IMEInventory<IAEItemStack>, IMEMonitorHandlerReceiver<AEEssentiaStack> {

        private final MEInventoryHandler<IAEItemStack> invHandler = new MEInventoryHandler<>(
            this,
            AEItemStackType.ITEM_STACK_TYPE);
        private IItemList<IAEItemStack> itemCache = null;

        PhialDiscretizingInventory() {
            invHandler.setPriority(Integer.MAX_VALUE);
        }

        @Override
        public IAEItemStack injectItems(IAEItemStack request, Actionable type, BaseActionSource src) {
            Aspect aspect = ItemPhial.getAspect(request);
            if (aspect == null) {
                return request;
            }
            IMEMonitor<AEEssentiaStack> essentiaGrid = getEssentiaGrid();
            if (essentiaGrid == null) {
                return request;
            }
            return ItemPhial.newAeStack(
                essentiaGrid.injectItems(ItemPhial.newEssentiaStack(aspect, request.getStackSize()), type, src));
        }

        @Override
        public IAEItemStack extractItems(IAEItemStack request, Actionable type, BaseActionSource src) {
            Aspect aspect = ItemPhial.getAspect(request);
            if (aspect == null) {
                return null;
            }
            IMEMonitor<AEEssentiaStack> essentiaGrid = getEssentiaGrid();
            if (essentiaGrid == null) {
                return null;
            }
            return ItemPhial.newAeStack(
                essentiaGrid.extractItems(ItemPhial.newEssentiaStack(aspect, request.getStackSize()), type, src));
        }

        @Override
        public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
            if (itemCache == null) {
                itemCache = AEApi.instance()
                    .storage()
                    .createItemList();
                IMEMonitor<AEEssentiaStack> essentiaGrid = getEssentiaGrid();
                if (essentiaGrid != null) {
                    for (AEEssentiaStack essentia : essentiaGrid.getStorageList()) {
                        IAEItemStack stack = ItemPhial.newAeStack(essentia);
                        if (stack != null) {
                            itemCache.add(stack);
                        }
                    }
                }
            }
            for (IAEItemStack stack : itemCache) {
                out.addStorage(stack);
            }
            return out;
        }

        @Override
        public IAEItemStack getAvailableItem(@Nonnull IAEItemStack request) {
            IMEMonitor<AEEssentiaStack> essentiaGrid = getEssentiaGrid();
            if (essentiaGrid == null) {
                return null;
            }
            AEEssentiaStack essentiaRequest = ItemPhial.getAeEssentiaStack(request);
            if (essentiaRequest == null) {
                return null;
            }
            AEEssentiaStack available = essentiaGrid.getAvailableItem(essentiaRequest, IterationCounter.fetchNewId());
            if (available == null || available.getAspect() == null) {
                return null;
            }
            return ItemPhial.newAeStack(available);
        }

        @Override
        public StorageChannel getChannel() {
            return StorageChannel.ITEMS;
        }

        @Override
        public boolean isValid(Object verificationToken) {
            IMEMonitor<AEEssentiaStack> essentiaGrid = getEssentiaGrid();
            return essentiaGrid != null && essentiaGrid == verificationToken;
        }

        @Override
        public void postChange(IBaseMonitor<AEEssentiaStack> monitor, Iterable<AEEssentiaStack> change,
            BaseActionSource actionSource) {
            itemCache = null;
            try {
                List<IAEItemStack> mappedChanges = new ArrayList<>();
                for (AEEssentiaStack essentia : change) {
                    IAEItemStack itemStack = ItemPhial.newAeStack(essentia);
                    if (itemStack != null) {
                        mappedChanges.add(itemStack);
                    }
                }
                getProxy().getGrid()
                    .<IStorageGrid>getCache(IStorageGrid.class)
                    .postAlterationOfStoredItems(AEItemStackType.ITEM_STACK_TYPE, mappedChanges, ownActionSource);
            } catch (GridAccessException e) {
                // NO-OP
            }
        }

        @Override
        public void onListUpdate() {
            // NO-OP
        }
    }
}
