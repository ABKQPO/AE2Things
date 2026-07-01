package com.asdflj.ae2thing.inventory.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.common.storage.RefreshableStorageMonitor;
import com.asdflj.ae2thing.inventory.ItemBiggerAppEngInventory;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.ITerminalTypeFilterProvider;
import appeng.api.storage.MEMonitorHandler;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.api.util.IConfigManager;
import appeng.container.interfaces.IInventorySlotAware;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.ConfigManager;
import appeng.util.IterationCounter;
import appeng.util.MonitorableTypeFilter;
import appeng.util.Platform;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;

public class BackpackTerminalInventory extends MEMonitorHandler<IAEItemStack>
    implements ITerminalHost, IInventorySlotAware, IGuiItemObject, IEnergySource, ITerminalTypeFilterProvider,
    RefreshableStorageMonitor {

    private final ItemStack target;
    private final int inventorySlot;
    protected AppEngInternalInventory crafting;
    protected EntityPlayer player;
    private final MonitorableTypeFilter typeFilters = new MonitorableTypeFilter();
    private IItemList<IAEItemStack> lastExternalSnapshot;

    @SuppressWarnings("unchecked")
    public BackpackTerminalInventory(ItemStack is, int slot, EntityPlayer player, IMEInventoryHandler<?> monitor) {
        super((IMEInventoryHandler<IAEItemStack>) monitor);
        this.target = is;
        this.inventorySlot = slot;
        this.player = player;
        this.crafting = new ItemBiggerAppEngInventory(is, Constants.CRAFTING, 9, player, slot);
        this.typeFilters.readFromNBT(Platform.openNbtData(this.target));
    }

    @Override
    public int getInventorySlot() {
        return this.inventorySlot;
    }

    @Override
    public ItemStack getItemStack() {
        return this.target;
    }

    @Override
    public IMEMonitor<IAEItemStack> getItemInventory() {
        return this;
    }

    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable mode, BaseActionSource src) {
        IAEItemStack result = super.injectItems(input, mode, src);
        if (mode == Actionable.MODULATE) {
            this.syncExternalSnapshot();
        }
        return result;
    }

    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, BaseActionSource src) {
        IAEItemStack result = super.extractItems(request, mode, src);
        if (mode == Actionable.MODULATE) {
            this.syncExternalSnapshot();
        }
        return result;
    }

    @Override
    public IItemList<IAEItemStack> getStorageList() {
        this.refreshExternalChanges(null);
        return super.getStorageList();
    }

    @Override
    public void refreshExternalChanges(BaseActionSource source) {
        IItemList<IAEItemStack> current = this.createCurrentSnapshot();
        if (this.lastExternalSnapshot == null) {
            this.lastExternalSnapshot = this.copySnapshot(current);
            return;
        }

        List<IAEStack<?>> changes = this.calculateChanges(this.lastExternalSnapshot, current);
        this.lastExternalSnapshot = this.copySnapshot(current);
        if (!changes.isEmpty()) {
            this.postChangesToListeners(changes, source);
        }
    }

    @Override
    public IMEMonitor<IAEFluidStack> getFluidInventory() {
        return null;
    }

    private void saveSettings() {
        this.player.inventory.setInventorySlotContents(this.inventorySlot, this.target);
    }

    @Override
    public IConfigManager getConfigManager() {
        final ConfigManager out = new ConfigManager((manager, settingName, newValue) -> {
            final NBTTagCompound data = Platform.openNbtData(this.target);
            manager.writeToNBT(data);
            saveSettings();
        });
        out.registerSetting(Settings.SORT_BY, SortOrder.NAME);
        out.registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
        out.registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);
        out.readFromNBT(
            (NBTTagCompound) Platform.openNbtData(this.target)
                .copy());
        return out;
    }

    public IInventory getInventoryByName(String crafting) {
        if (crafting.equals(Constants.CRAFTING)) {
            return this.crafting;
        }
        return null;
    }

    @Override
    public double extractAEPower(double amt, Actionable mode, PowerMultiplier usePowerMultiplier) {
        amt = usePowerMultiplier.multiply(amt);
        return usePowerMultiplier.divide(amt);
    }

    @Override
    public Reference2BooleanMap<IAEStackType<?>> getTypeFilter(EntityPlayer player) {
        return this.typeFilters.getFilters(player);
    }

    @Override
    public void saveTypeFilter() {
        this.typeFilters.writeToNBT(this.target);
        this.saveSettings();
    }

    private void syncExternalSnapshot() {
        this.lastExternalSnapshot = this.copySnapshot(this.createCurrentSnapshot());
    }

    private IItemList<IAEItemStack> createCurrentSnapshot() {
        IItemList<IAEItemStack> current = AEApi.instance()
            .storage()
            .createItemList();
        this.getHandler()
            .getAvailableItems(current, IterationCounter.fetchNewId());
        return current;
    }

    private IItemList<IAEItemStack> copySnapshot(IItemList<IAEItemStack> source) {
        IItemList<IAEItemStack> copy = AEApi.instance()
            .storage()
            .createItemList();
        for (IAEItemStack stack : source) {
            copy.addStorage(stack.copy());
        }
        return copy;
    }

    private List<IAEStack<?>> calculateChanges(IItemList<IAEItemStack> previous, IItemList<IAEItemStack> current) {
        List<IAEStack<?>> changes = new ArrayList<>();
        for (IAEItemStack currentStack : current) {
            IAEItemStack previousStack = previous.findPrecise(currentStack);
            long previousSize = previousStack == null ? 0 : previousStack.getStackSize();
            long difference = currentStack.getStackSize() - previousSize;
            if (difference != 0) {
                IAEItemStack change = currentStack.copy();
                change.setStackSize(difference);
                changes.add(change);
            }
        }
        for (IAEItemStack previousStack : previous) {
            if (current.findPrecise(previousStack) == null) {
                IAEItemStack change = previousStack.copy();
                change.setStackSize(-previousStack.getStackSize());
                changes.add(change);
            }
        }
        return changes;
    }
}
