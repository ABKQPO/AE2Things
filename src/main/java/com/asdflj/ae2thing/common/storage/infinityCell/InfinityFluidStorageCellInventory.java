package com.asdflj.ae2thing.common.storage.infinityCell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.asdflj.ae2thing.api.AE2ThingAPI;
import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.common.item.BaseCellItem;
import com.asdflj.ae2thing.common.storage.DataStorage;
import com.asdflj.ae2thing.common.storage.ITFluidCellInventory;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.exceptions.AppEngException;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.implementations.tiles.IChestOrDrive;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IItemList;
import appeng.util.Platform;

public class InfinityFluidStorageCellInventory implements ITFluidCellInventory {

    protected static final String FLUID_TYPE_TAG = "ft";
    protected static final String FLUID_COUNT_TAG = "fc";
    private final IChestOrDrive drive;
    protected IStorageCell cellType;
    protected final ItemStack cellItem;
    protected final ISaveProvider container;
    protected long storedFluidCount;
    protected long storedFluids;
    protected IItemList<IAEFluidStack> cellFluids = null;
    protected final NBTTagCompound data;
    protected final DataStorage storage;

    public InfinityFluidStorageCellInventory(ItemStack o, ISaveProvider c, EntityPlayer player) throws AppEngException {
        if (o == null) {
            throw new AppEngException("ItemStack was used as a cell, but was not a cell!");
        }
        this.drive = c instanceof IChestOrDrive ? (IChestOrDrive) c : null;
        this.cellItem = o;
        this.cellType = (IStorageCell) this.cellItem.getItem();
        this.container = c;
        this.data = Platform.openNbtData(o);
        this.storedFluids = this.data.getLong(FLUID_TYPE_TAG);
        this.storedFluidCount = this.data.getLong(FLUID_COUNT_TAG);
        this.storage = this.getStorage();
    }

    @Override
    public IAEFluidStack injectItems(IAEFluidStack input, Actionable mode, BaseActionSource src) {
        if (input == null) {
            return null;
        }
        if (input.getStackSize() == 0) {
            return null;
        }
        if (this.cellType.isBlackListed(input)) {
            return input;
        }
        final IAEFluidStack l = this.getCellFluids()
            .findPrecise(input);

        if (l != null) {
            if (mode == Actionable.MODULATE) {
                l.setStackSize(l.getStackSize() + input.getStackSize());
                this.saveChanges(input.copy());
            }
            return null;
        }

        if (this.canHoldNewItem()) // room for new type, and for at least one item!
        {
            if (mode == Actionable.MODULATE) {
                this.cellFluids.add(input);
                this.saveChanges(input.copy());
            }
            return null;
        }

        return input;
    }

    @Override
    public IAEFluidStack extractItems(IAEFluidStack request, Actionable mode, BaseActionSource src) {
        if (request == null) {
            return null;
        }

        final long size = request.getStackSize();

        IAEFluidStack results = null;

        final IAEFluidStack l = this.getCellFluids()
            .findPrecise(request);

        if (l != null) {
            results = l.copy();

            if (l.getStackSize() <= size) {
                results.setStackSize(l.getStackSize());

                if (mode == Actionable.MODULATE) {
                    l.setStackSize(0);
                    IAEFluidStack change = results.copy();
                    change.setStackSize(-results.getStackSize());
                    this.saveChanges(change);
                }
            } else {
                results.setStackSize(size);

                if (mode == Actionable.MODULATE) {
                    l.setStackSize(l.getStackSize() - size);
                    IAEFluidStack change = results.copy();
                    change.setStackSize(-results.getStackSize());
                    this.saveChanges(change);
                }
            }
        }

        return results;
    }

    private void saveChanges(IAEFluidStack change) {
        this.data.setBoolean(Constants.IS_EMPTY, this.cellFluids.isEmpty());
        if (this.container != null) {
            this.container.saveChanges(this);
        }
        AE2ThingAPI.instance()
            .getStorageManager()
            .postFluidChange(this.storage, this.drive, change);
        AE2ThingAPI.instance()
            .getStorageManager()
            .setDirty(true);
    }

    @Override
    public IItemList<IAEFluidStack> getAvailableItems(IItemList<IAEFluidStack> out, int iteration) {
        AE2ThingAPI.instance()
            .getStorageManager()
            .addGrid(this.getUUID(), this.drive);
        for (final IAEFluidStack i : this.getCellFluids()) {
            out.add(i);
        }
        return out;
    }

    protected IItemList<IAEFluidStack> getCellFluids() {
        if (this.cellFluids == null) {
            this.loadCellFluids();
        }
        return this.cellFluids;
    }

    protected void loadCellFluids() {
        if (this.cellFluids == null) {
            this.cellFluids = this.storage.getFluids();
            for (IAEFluidStack is : this.cellFluids) {
                if (is.getStackSize() <= 0) is.reset();
            }
        }
        if (!this.getUUID()
            .equals(this.storage.getUUID())) {
            data.setString(Constants.DISKUUID, this.storage.getUUID());
        }
    }

    @Override
    public String getUUID() {
        if (data.hasNoTags()) {
            return "";
        }
        return data.getString(Constants.DISKUUID);
    }

    @Override
    public StorageChannel getChannel() {
        return ((BaseCellItem) Objects.requireNonNull(this.cellItem.getItem())).getChannel();
    }

    @Override
    public ItemStack getItemStack() {
        return this.cellItem;
    }

    @Override
    public double getIdleDrain() {
        return this.cellType.getIdleDrain(this.cellItem);
    }

    @Override
    public double getIdleDrain(ItemStack is) {
        return this.cellType.getIdleDrain(is);
    }

    @Override
    public FuzzyMode getFuzzyMode() {
        return this.cellType.getFuzzyMode(this.cellItem);
    }

    @Override
    public IInventory getConfigInventory() {
        return this.cellType.getConfigInventory(this.cellItem);
    }

    @Override
    public int getBytesPerType() {
        return this.cellType.getBytesPerType(this.cellItem);
    }

    @Override
    public boolean canHoldNewItem() {
        return true;
    }

    @Override
    public long getTotalBytes() {
        return this.cellType.getBytes(this.cellItem);
    }

    @Override
    public long getFreeBytes() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long getUsedBytes() {
        return 0;
    }

    @Override
    public long getStoredItemCount() {
        return this.storedFluidCount;
    }

    @Override
    public long getRemainingItemCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long getRemainingItemTypes() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getUnusedItemCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getStatusForCell() {
        if (this.canHoldNewItem()) {
            return 1;
        }
        if (this.getRemainingItemCount() > 0) {
            return 2;
        }
        return 3;
    }

    @Override
    public long getStoredItemTypes() {
        return this.storedFluids;
    }

    @Override
    public long getTotalItemTypes() {
        return this.cellType.getTotalTypes(this.cellItem);
    }

    @Override
    public String getOreFilter() {
        return this.cellType.getOreFilter(this.cellItem);
    }

    @Override
    public List<IAEFluidStack> getContents() {
        if (Platform.isClient()) return Collections.emptyList();
        List<IAEFluidStack> ret = new ArrayList<>();
        for (IAEFluidStack fluid : this.getCellFluids()) {
            ret.add(fluid);
        }
        return ret;
    }

    @Override
    public IAEFluidStack getAvailableItem(@Nonnull IAEFluidStack request, int iteration) {
        IAEFluidStack available = this.getCellFluids()
            .findPrecise(request);
        return available == null ? null : available.copy();
    }

    @Override
    public IInventory getUpgradesInventory() {
        return this.cellType.getUpgradesInventory(this.cellItem);
    }

}
