package com.asdflj.ae2thing.common.storage.infinityCell;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.asdflj.ae2thing.api.AE2ThingAPI;
import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.common.item.ItemInfinityStorageCell;
import com.asdflj.ae2thing.common.storage.DataStorage;
import com.asdflj.ae2thing.common.storage.ITCellInventory;
import com.gtnewhorizon.gtnhlib.item.ItemStackNBT;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.exceptions.AppEngException;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.implementations.tiles.IChestOrDrive;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.IterationCounter;
import appeng.util.item.AEItemStack;
import appeng.util.item.AEItemStackType;

public class InfinityItemStorageCellInventory implements ITCellInventory {

    private static final String ITEM_TYPE_TAG = "it";
    private static final String ITEM_COUNT_TAG = "ic";
    protected final ItemStack cellItem;
    protected ItemInfinityStorageCell cellType;
    protected final ISaveProvider container;
    protected final EntityPlayer player;
    protected IItemList<IAEItemStack> cellItems = null;
    protected long storedItemTypes;
    protected long storedItemCount;
    protected final ItemStackNBT data;
    protected final IChestOrDrive drive;
    protected final DataStorage storage;

    public InfinityItemStorageCellInventory(ItemStack o, ISaveProvider c, EntityPlayer p) throws AppEngException {
        if (o == null) {
            throw new AppEngException("ItemStack was used as a cell, but was not a cell!");
        }
        cellItem = o;
        container = c;
        player = p;
        this.drive = c instanceof IChestOrDrive ? (IChestOrDrive) c : null;
        this.cellType = (ItemInfinityStorageCell) this.cellItem.getItem();
        this.data = ItemStackNBT.of(this.cellItem);
        this.storedItemTypes = data.getLong(ITEM_TYPE_TAG);
        this.storedItemCount = data.getLong(ITEM_COUNT_TAG);
        this.storage = this.getStorage();
    }

    @Override
    public double getIdleDrain(ItemStack is) {
        return this.cellType.getIdleDrain();
    }

    @Override
    public void loadCellItems() {
        if (this.cellItems == null) {
            this.cellItems = this.storage == null ? AEApi.instance()
                .storage()
                .createItemList() : this.storage.getItems();
            for (IAEItemStack is : this.cellItems) {
                if (is.getStackSize() <= 0) is.reset();
            }
        }
        if (this.storage != null && !this.getUUID()
            .equals(this.storage.getUUID())) {
            data.setString(Constants.DISKUUID, this.storage.getUUID());
        }
    }

    protected IItemList<IAEItemStack> getCellItems() {
        if (this.cellItems == null) {
            this.loadCellItems();
        }
        return this.cellItems;
    }

    @Override
    public String getUUID() {
        if (data.hasNoTags()) {
            return "";
        }
        return data.getString(Constants.DISKUUID);
    }

    @Override
    public ItemStack getItemStack() {
        return this.cellItem;
    }

    @Override
    public FuzzyMode getFuzzyMode() {
        return this.cellType.getFuzzyMode(this.cellItem);
    }

    @Override
    public IAEStackInventory getConfigAEInventory() {
        return this.cellType.getConfigAEInventory(this.cellItem);
    }

    @Override
    public IInventory getUpgradesInventory() {
        return this.cellType.getUpgradesInventory(this.cellItem);
    }

    @Override
    public int getBytesPerType() {
        return this.cellType.getBytesPerType(this.cellItem);
    }

    @Override
    public boolean canHoldNewItem(ItemStack is) {
        return true;
    }

    @Override
    public long getTotalBytes() {
        return this.cellType.getTotalTypes(this.cellItem);
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
    public long getTotalItemTypes() {
        return this.cellType.getTotalTypes(this.cellItem);
    }

    @Override
    public long getStoredItemCount() {
        return this.storedItemCount;
    }

    @Override
    public long getStoredItemTypes() {
        return this.storedItemTypes;
    }

    @Override
    public long getRemainingItemTypes() {
        return Integer.MAX_VALUE;
    }

    @Override
    public long getRemainingItemCount() {
        return Long.MAX_VALUE;
    }

    @Override
    public int getUnusedItemCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getStatusForCell() {
        if (this.canHoldNewItem(this.cellItem)) {
            return 1;
        }
        if (this.getRemainingItemCount() > 0) {
            return 2;
        }
        return 3;
    }

    @Override
    public String getOreFilter() {
        return this.cellType.getOreFilter(this.cellItem);
    }

    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable mode, BaseActionSource src) {
        if (input == null) {
            return null;
        }

        if (input.getStackSize() == 0) {
            return null;
        }

        if (this.cellType.isBlackListed(input)) {
            return input;
        }

        final ItemStack sharedItemStack = input.getItemStack();

        if (sharedItemStack != null && sharedItemStack.getItem() instanceof IStorageCell cell
            && !cell.storableInStorageCell()) {
            final IMEInventoryHandler<?> handler = AEApi.instance()
                .registries()
                .cell()
                .getCellInventory(sharedItemStack, null, cell.getStackType());
            if (handler != null && !isEmpty(handler)) {
                return input;
            }
        }

        final IAEItemStack l = this.getCellItems()
            .findPrecise(input);

        if (l != null) {
            final long remainingItemSlots = this.getRemainingItemCount();

            if (remainingItemSlots < 0) {
                return input;
            }

            if (input.getStackSize() > remainingItemSlots) {
                final IAEItemStack r = input.copy();
                r.setStackSize(r.getStackSize() - remainingItemSlots);

                if (mode == Actionable.MODULATE) {
                    l.setStackSize(l.getStackSize() + remainingItemSlots);
                    IAEItemStack change = input.copy();
                    change.setStackSize(remainingItemSlots);
                    this.saveChanges(change);
                }

                return r;
            } else {
                if (mode == Actionable.MODULATE) {
                    l.setStackSize(l.getStackSize() + input.getStackSize());
                    this.saveChanges(input.copy());
                }

                return null;
            }
        }

        if (this.canHoldNewItem(this.cellItem)) // room for new type, and for at least one item!
        {
            final long remainingItemCount = this.getRemainingItemCount() - this.getBytesPerType() * 8L;

            if (remainingItemCount > 0) {
                if (input.getStackSize() > remainingItemCount) {
                    final IAEItemStack toReturn = AEItemStack.create(sharedItemStack);
                    toReturn.decStackSize(remainingItemCount);

                    if (mode == Actionable.MODULATE) {
                        final IAEItemStack toWrite = AEItemStack.create(sharedItemStack);
                        toWrite.setStackSize(remainingItemCount);
                        this.cellItems.add(toWrite);
                        this.saveChanges(toWrite.copy());
                    }
                    return toReturn;
                }

                if (mode == Actionable.MODULATE) {
                    this.cellItems.add(input);
                    this.saveChanges(input.copy());
                }

                return null;
            }
        }

        return input;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean isEmpty(IMEInventory meInventory) {
        return meInventory.getAvailableItems(
            meInventory.getStackType()
                .createList(),
            IterationCounter.fetchNewId())
            .isEmpty();
    }

    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, BaseActionSource src) {
        if (request == null) {
            return null;
        }

        final long size = request.getStackSize();

        IAEItemStack results = null;

        final IAEItemStack l = this.getCellItems()
            .findPrecise(request);

        if (l != null) {
            results = l.copy();

            if (l.getStackSize() <= size) {
                results.setStackSize(l.getStackSize());

                if (mode == Actionable.MODULATE) {
                    l.setStackSize(0);
                    IAEItemStack change = results.copy();
                    change.setStackSize(-results.getStackSize());
                    this.saveChanges(change);
                }
            } else {
                results.setStackSize(size);

                if (mode == Actionable.MODULATE) {
                    l.setStackSize(l.getStackSize() - size);
                    IAEItemStack change = results.copy();
                    change.setStackSize(-results.getStackSize());
                    this.saveChanges(change);
                }
            }
        }

        return results;
    }

    private void saveChanges(IAEItemStack change) {
        this.data.setBoolean(Constants.IS_EMPTY, this.cellItems.isEmpty());
        if (this.container != null) {
            this.container.saveChanges(this);
        }
        AE2ThingAPI.instance()
            .getStorageManager()
            .postItemChange(this.storage, this.drive, change);
        AE2ThingAPI.instance()
            .getStorageManager()
            .setDirty(true);
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out, int iteration) {
        if (this.storage != null && AE2ThingAPI.instance()
            .getStorageManager() != null) {
            AE2ThingAPI.instance()
                .getStorageManager()
                .addGrid(this.getUUID(), this.drive);
        }
        for (final IAEItemStack i : this.getCellItems()) {
            out.add(i);
        }
        return out;
    }

    @Override
    public IAEItemStack getAvailableItem(@NotNull IAEItemStack request, int iteration) {
        IAEItemStack is = this.getCellItems()
            .findPrecise(request);
        if (is != null) return is.copy();
        return null;
    }

    @Override
    @NotNull
    public IAEStackType<?> getStackType() {
        return AEItemStackType.ITEM_STACK_TYPE;
    }
}
