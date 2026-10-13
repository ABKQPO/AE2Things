package com.asdflj.ae2thing.common.storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldSavedData;

import com.asdflj.ae2thing.api.AE2ThingAPI;
import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.common.Config;
import com.asdflj.ae2thing.common.item.BaseCellItem;
import com.gtnewhorizon.gtnhlib.item.ItemStackNBT;

import appeng.api.implementations.tiles.IChestOrDrive;
import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.AEStackTypeRegistry;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.core.AELog;
import appeng.tile.grid.AENetworkInvTile;
import appeng.util.item.AEFluidStackType;
import appeng.util.item.AEItemStackType;

public class StorageManager extends WorldSavedData {

    private final Map<UUID, DataStorage> disks = new HashMap<>();
    private final Map<UUID, Set<IGrid>> grids = new HashMap<>();

    public StorageManager(String name) {
        super(name);
        this.setDirty(true);
        AE2ThingAPI.instance()
            .setStorageManager(this);
    }

    private DataStorage getStorage(String uuid, IAEStackType<?> stackType) {
        UUID uid;
        DataStorage d;
        try {
            uid = UUID.fromString(uuid);
        } catch (Exception ignored) {
            do {
                uid = UUID.randomUUID();
            } while (disks.get(uid) != null);
        }
        d = disks.get(uid);
        if (d == null) {
            d = new DataStorage(uid, stackType);
            disks.put(uid, d);
        }
        return d;
    }

    private void addGrid(UUID uid, IGrid grid) {
        if (!Config.cellLink || grid == null || grid.isEmpty()) return;
        this.grids.putIfAbsent(uid, new HashSet<>());
        this.grids.get(uid)
            .add(grid);
    }

    public void addGrid(String uuid, IGrid grid) {
        UUID uid = UUID.fromString(uuid);
        this.addGrid(uid, grid);
    }

    public void addGrid(String uuid, IChestOrDrive drive) {
        if (drive == null || uuid.isEmpty()) return;
        this.addGrid(
            uuid,
            drive.getActionableNode()
                .getGrid());
    }

    public DataStorage getStorage(ItemStack item) {
        if (item.getItem() instanceof BaseCellItem bci) {
            ItemStackNBT data = ItemStackNBT.of(item);
            return this.getStorage(data.getString(Constants.DISKUUID), bci.getStackType());
        }
        return null;
    }

    public DataStorage getStorage(ItemStack item, EntityPlayer player) {
        DataStorage storage = this.getStorage(item);
        if (storage == null) return null;
        ItemStackNBT data = ItemStackNBT.of(item);
        String uuid = data.getString(Constants.DISKUUID);
        if (uuid.isEmpty()) {
            data.setString(Constants.DISKUUID, storage.getUUID());
            player.inventory.setInventorySlotContents(player.inventory.currentItem, item.copy());
        }
        return storage;
    }

    public void setStorage(String uuid, ItemStack item) {
        ItemStackNBT data = ItemStackNBT.of(item);
        if (!data.getBoolean(Constants.LINKED) && !this.getStorage(item)
            .isEmpty()) return;
        String curUid = data.getString(Constants.DISKUUID);
        if (!curUid.isEmpty() && curUid.equals(uuid)) return;
        data.setString(Constants.DISKUUID, uuid);
        data.setBoolean(Constants.LINKED, true);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        final Map<UUID, DataStorage> d = new HashMap<>();
        try {
            final NBTTagList diskList = data.getTagList(Constants.DISKLIST, 10);
            for (int i = 0; i < diskList.tagCount(); i++) {
                NBTTagCompound disk = diskList.getCompoundTagAt(i);
                UUID uid = this.parseDiskUUID(disk);
                if (uid == null) continue;
                d.put(
                    uid,
                    DataStorage
                        .readFromNBT(uid, disk.getTagList(Constants.DISKDATA, 10), AEItemStackType.ITEM_STACK_TYPE));
            }
            NBTTagList fluidDiskList = data.getTagList(Constants.FLUID_DISKLIST, 10);
            for (int i = 0; i < fluidDiskList.tagCount(); i++) {
                NBTTagCompound disk = fluidDiskList.getCompoundTagAt(i);
                UUID uid = this.parseDiskUUID(disk);
                if (uid == null) continue;
                d.put(
                    uid,
                    DataStorage.readFromNBT(
                        uid,
                        disk.getTagList(Constants.FLUID_DISKLIST, 10),
                        AEFluidStackType.FLUID_STACK_TYPE));
            }
            NBTTagList otherDiskList = data.getTagList(Constants.OTHER_DISKLIST, 10);
            for (int i = 0; i < otherDiskList.tagCount(); i++) {
                NBTTagCompound disk = otherDiskList.getCompoundTagAt(i);
                UUID uid = this.parseDiskUUID(disk);
                if (uid == null) continue;
                IAEStackType<?> type = AEStackTypeRegistry.getType(disk.getString(Constants.DISKTYPE));
                if (type == null) continue;
                d.put(uid, DataStorage.readFromNBT(uid, disk.getTagList(Constants.OTHER_DISKLIST, 10), type));
            }
        } catch (Exception e) {
            AELog.error(e, "[AE2Thing] Failed to read StorageManager data, keeping previous state");
            return;
        }
        this.disks.clear();
        this.disks.putAll(d);
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        NBTTagList diskList = new NBTTagList();
        NBTTagList fluidDiskList = new NBTTagList();
        NBTTagList otherDiskList = new NBTTagList();
        for (Map.Entry<UUID, DataStorage> entry : disks.entrySet()) {
            if (entry.getValue()
                .isEmpty()) continue;
            final NBTTagCompound disk = new NBTTagCompound();

            disk.setString(
                Constants.DISKUUID,
                entry.getKey()
                    .toString());
            IAEStackType<?> type = entry.getValue()
                .getStackType();
            if (type != null) {
                disk.setString(Constants.DISKTYPE, type.getId());
            }
            if (entry.getValue()
                .getStackType() == AEItemStackType.ITEM_STACK_TYPE) {
                disk.setTag(
                    Constants.DISKDATA,
                    entry.getValue()
                        .writeToNBT());
                diskList.appendTag(disk);
            } else if (entry.getValue()
                .getStackType() == AEFluidStackType.FLUID_STACK_TYPE) {
                    disk.setTag(
                        Constants.FLUID_DISKLIST,
                        entry.getValue()
                            .writeToNBT());
                    fluidDiskList.appendTag(disk);
                } else {
                    disk.setTag(
                        Constants.OTHER_DISKLIST,
                        entry.getValue()
                            .writeToNBT());
                    otherDiskList.appendTag(disk);
                }

        }
        data.setTag(Constants.DISKLIST, diskList);
        data.setTag(Constants.FLUID_DISKLIST, fluidDiskList);
        data.setTag(Constants.OTHER_DISKLIST, otherDiskList);
    }

    public void postItemChange(final DataStorage storage, IChestOrDrive drive, IAEItemStack change) {
        if (change == null || change.getStackSize() == 0) return;
        this.postChanges(
            storage,
            drive,
            storageGrid -> storageGrid
                .postAlterationOfStoredItems(AEItemStackType.ITEM_STACK_TYPE, Collections.singletonList(change), null));
    }

    public void postFluidChange(final DataStorage storage, IChestOrDrive drive, IAEFluidStack change) {
        if (change == null || change.getStackSize() == 0) return;
        this.postChanges(
            storage,
            drive,
            storageGrid -> storageGrid.postAlterationOfStoredItems(
                AEFluidStackType.FLUID_STACK_TYPE,
                Collections.singletonList(change),
                null));
    }

    public void postOtherChange(final DataStorage storage, IChestOrDrive drive, IAEStack<?> change) {
        if (change == null || change.getStackSize() == 0) return;
        final IAEStackType<?> type = change.getStackType();
        if (type == null) return;
        this.postChanges(
            storage,
            drive,
            storageGrid -> storageGrid.postAlterationOfStoredItems(type, Collections.singleton(change), null));
    }

    private void postChanges(final DataStorage storage, IChestOrDrive drive, Consumer<IStorageGrid> postChange) {
        if (!Config.cellLink || drive == null) return;
        if (drive instanceof AENetworkInvTile ait) {
            try {
                IGrid curGrid = ait.getProxy()
                    .getGrid();
                UUID uid = storage.getRawUUID();
                if (curGrid.isEmpty()) return;
                Set<IGrid> iGrids = this.grids.get(uid);
                if (iGrids == null) return;
                iGrids.removeIf(
                    g -> g.isEmpty() || !g.getPivot()
                        .isActive());
                if (iGrids.size() <= 1) return;
                Set<IGrid> cp = new HashSet<>(iGrids);
                for (IGrid grid : cp) {
                    if (grid.isEmpty() || grid.equals(curGrid)) continue;
                    postChange.accept(grid.getCache(IStorageGrid.class));
                }
            } catch (Exception ignored) {}
        }
    }

    private UUID parseDiskUUID(NBTTagCompound tag) {
        final String raw = tag.getString(Constants.DISKUUID);
        if (raw.isEmpty()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            AELog.warn(e, "[AE2Thing] Skipping corrupted disk entry, invalid uuid: " + raw);
            return null;
        }
    }
}
