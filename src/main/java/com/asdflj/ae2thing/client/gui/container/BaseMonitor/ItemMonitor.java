package com.asdflj.ae2thing.client.gui.container.BaseMonitor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ICrafting;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.common.storage.RefreshableStorageMonitor;
import com.asdflj.ae2thing.network.SPacketMEItemInvUpdate;
import com.glodblock.github.common.item.ItemFluidDrop;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingPostPatternChangeListener;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.me.cache.CraftingGridCache;
import appeng.util.Platform;

public class ItemMonitor
    implements IMEMonitorHandlerReceiver<IAEItemStack>, IProcessItemList, ICraftingPostPatternChangeListener {

    private IMEMonitor<IAEItemStack> itemMonitor;
    private final IItemList<IAEItemStack> items = AEApi.instance()
        .storage()
        .createItemList();
    private final List<ICrafting> crafters;
    private FluidMonitor fluidMonitorObject = null;
    private CraftingGridCache craftingGridCache = null;
    /** Craftable items the clients currently know about, used to notice items that stopped being craftable. */
    private IItemList<IAEItemStack> lastCraftable = AEApi.instance()
        .storage()
        .createItemList();
    private boolean craftableDirty = false;

    public ItemMonitor(List<ICrafting> crafters) {
        this.crafters = crafters;
    }

    public void setMonitor(IMEMonitor<IAEItemStack> itemMonitor) {
        this.itemMonitor = itemMonitor;
    }

    public void setFluidMonitorObject(FluidMonitor objectMonitor) {
        this.fluidMonitorObject = objectMonitor;
    }

    @Override
    public void addListener() {
        this.itemMonitor.addListener(this, null);
    }

    @Override
    public boolean isValid(Object verificationToken) {
        return this.itemMonitor != null;
    }

    @Override
    public void postChange(IBaseMonitor<IAEItemStack> monitor, Iterable<IAEItemStack> change,
        BaseActionSource actionSource) {
        for (final IAEItemStack is : change) {
            this.items.add(is);
        }
    }

    @Override
    public void onListUpdate() {
        for (final Object c : this.crafters) {
            if (c instanceof final ICrafting cr) {
                this.queueInventory(cr);
            }
        }
    }

    private void fluidHandler(IAEItemStack send) {
        if (this.fluidMonitorObject != null && send.getStackSize() == 0 && send.getItem() instanceof ItemFluidDrop) {
            this.fluidMonitorObject.addItemCraftingFluid(send);
        }
    }

    @Override
    public void processItemList() {
        if (this.items.isEmpty() && !this.craftableDirty) {
            return;
        }
        this.craftableDirty = false;
        IItemList<IAEItemStack> monitorCache = null;
        if (this.itemMonitor instanceof RefreshableStorageMonitor refreshable) {
            monitorCache = refreshable.refreshExternalChanges(null, false);
        }
        if (monitorCache == null) {
            monitorCache = this.itemMonitor.getStorageList();
        }
        List<IAEItemStack> toSend = new ArrayList<>();
        for (final IAEItemStack is : this.items) {
            IAEItemStack send = monitorCache.findPrecise(is);
            if (send != null) {
                fluidHandler(send.copy());
                toSend.add(send);
            } else {
                is.setStackSize(0);
                toSend.add(is);
            }
        }
        this.items.resetStatus();
        appendLostCraftables(monitorCache, toSend);
        if (toSend.isEmpty()) {
            return;
        }
        SPacketMEItemInvUpdate piu = new SPacketMEItemInvUpdate();
        piu.addAll(toSend);
        for (final Object c : this.crafters) {
            if (c instanceof EntityPlayer) {
                AE2Thing.proxy.netHandler.sendTo(piu, (EntityPlayerMP) c);
            }
        }
    }

    /**
     * Tells the clients about items that stopped being craftable.
     * <p>
     * {@code CraftingGridCache.updatePatterns()} only ever publishes the patterns that exist right now, so when the
     * last pattern producing an item is removed that item never reaches the change queue at all. The client keeps the
     * entry it was last sent, which still carries {@code craftable = true}, so the item stays marked as craftable in
     * the
     * item panel forever. Diffing against the previously published craftable set lets us push an explicitly unmarked
     * entry for it instead. This is invisible in plain AE2 terminals because they never show an item panel next to the
     * interface terminal.
     */
    private void appendLostCraftables(IItemList<IAEItemStack> monitorCache, List<IAEItemStack> toSend) {
        final IItemList<IAEItemStack> current = AEApi.instance()
            .storage()
            .createItemList();
        for (final IAEItemStack is : monitorCache) {
            if (is != null && is.isCraftable()) {
                current.add(is);
            }
        }
        for (final IAEItemStack was : this.lastCraftable) {
            if (was == null || current.findPrecise(was) != null) {
                continue;
            }
            final IAEItemStack now = monitorCache.findPrecise(was);
            final IAEItemStack clear = now != null ? now.copy() : was.copy();
            clear.setCraftable(false);
            if (now == null) {
                clear.setStackSize(0);
            }
            toSend.add(clear);
        }
        this.lastCraftable = current;
    }

    @Override
    public void onPostPatternChange() {
        this.craftableDirty = true;
    }

    /**
     * Starts watching the network for pattern changes, so the item panel learns about items that stop being craftable.
     */
    public void registerCraftableListener(IGrid grid) {
        if (grid == null || this.craftingGridCache != null) {
            return;
        }
        if (grid.getCache(ICraftingGrid.class) instanceof CraftingGridCache cache) {
            this.craftingGridCache = cache;
            cache.addPostPatternChangeListeners(this);
        }
    }

    public void unregisterCraftableListener() {
        if (this.craftingGridCache != null) {
            this.craftingGridCache.removePostPatternChangeListeners(this);
            this.craftingGridCache = null;
        }
    }

    @Override
    public void queueInventory(ICrafting c) {
        if (Platform.isServer() && c instanceof EntityPlayer && this.itemMonitor != null) {
            final IItemList<IAEItemStack> monitorCache = this.itemMonitor instanceof RefreshableStorageMonitor refreshable
                ? refreshable.refreshExternalChanges(null, true)
                : this.itemMonitor.getStorageList();
            List<IAEItemStack> toSend = new ArrayList<>();
            for (final IAEItemStack is : monitorCache) {
                fluidHandler(is.copy());
                toSend.add(is);
            }
            appendLostCraftables(monitorCache, toSend);
            SPacketMEItemInvUpdate piu = new SPacketMEItemInvUpdate();
            piu.addAll(toSend);
            AE2Thing.proxy.netHandler.sendTo(piu, (EntityPlayerMP) c);
        }
    }

    @Override
    public void removeCraftingFromCrafters(ICrafting c) {
        if (this.crafters.isEmpty() && this.itemMonitor != null) {
            this.itemMonitor.removeListener(this);
        }
    }

    @Override
    public void removeListener() {
        if (this.itemMonitor != null) this.itemMonitor.removeListener(this);
    }

    public IMEMonitor<IAEItemStack> getMonitor() {
        return this.itemMonitor;
    }
}
