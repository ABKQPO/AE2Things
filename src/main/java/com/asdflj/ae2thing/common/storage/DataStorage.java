package com.asdflj.ae2thing.common.storage;

import java.util.UUID;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEFluidStackType;
import appeng.util.item.AEItemStack;
import appeng.util.item.AEItemStackType;

public class DataStorage implements IDataStorage {

    private IItemList<IAEItemStack> items;
    private IItemList<IAEFluidStack> fluids;
    private IItemList<IAEStack<?>> others;
    private final UUID uuid;
    private final IAEStackType<?> stackType;

    public DataStorage(UUID uuid, IAEStackType<?> stackType) {
        this.uuid = uuid;
        this.stackType = stackType;
    }

    @Override
    public IAEStackType<?> getStackType() {
        return this.stackType;
    }

    @Override
    public IItemList<IAEItemStack> getItems() {
        if (this.items == null) {
            this.items = AEApi.instance()
                .storage()
                .createPrimitiveItemList();
        }
        return items;
    }

    @Override
    public IItemList<IAEFluidStack> getFluids() {
        if (this.fluids == null) {
            this.fluids = AEApi.instance()
                .storage()
                .createFluidList();
        }
        return fluids;
    }

    @Override
    public IItemList<IAEStack<?>> getOthers() {
        if (this.others == null) {
            this.others = AEApi.instance()
                .storage()
                .createAEStackList();
        }
        return others;
    }

    @Override
    public boolean isEmpty() {
        if (this.getStackType() == AEItemStackType.ITEM_STACK_TYPE) {
            return this.getItems()
                .isEmpty();
        } else if (this.getStackType() == AEFluidStackType.FLUID_STACK_TYPE) {
            return this.getFluids()
                .isEmpty();
        } else {
            return this.getOthers()
                .isEmpty();
        }
    }

    @Override
    public String getUUID() {
        return this.uuid.toString();
    }

    @Override
    public UUID getRawUUID() {
        return this.uuid;
    }

    public static DataStorage readFromNBT(UUID uuid, NBTTagList data) {
        return readFromNBT(uuid, data, AEItemStackType.ITEM_STACK_TYPE);
    }

    public static DataStorage readFromNBT(UUID uuid, NBTTagList data, IAEStackType<?> stackType) {
        DataStorage storage = new DataStorage(uuid, stackType);
        storage.readFromNBT(data);
        return storage;
    }

    @Override
    public void readFromNBT(NBTTagList data) {
        if (this.getStackType() == AEItemStackType.ITEM_STACK_TYPE) {
            for (final IAEItemStack ais : this.readList(data)) {
                this.getItems()
                    .add(ais);
            }
        } else if (this.getStackType() == AEFluidStackType.FLUID_STACK_TYPE) {
            for (final IAEFluidStack ais : this.readFluidList(data)) {
                this.getFluids()
                    .add(ais);
            }
        } else {
            for (final IAEStack<?> as : this.readOtherList(data)) {
                this.getOthers()
                    .add(as);
            }
        }
    }

    private IItemList<IAEStack<?>> readOtherList(final NBTTagList tag) {
        final IItemList<IAEStack<?>> out = AEApi.instance()
            .storage()
            .createAEStackList();
        if (tag == null) {
            return out;
        }

        for (int x = 0; x < tag.tagCount(); x++) {
            final IAEStack<?> ais = IAEStack.fromNBTGeneric(tag.getCompoundTagAt(x));
            if (ais != null) {
                out.add(ais);
            }
        }

        return out;
    }

    private IItemList<IAEFluidStack> readFluidList(final NBTTagList tag) {
        final IItemList<IAEFluidStack> out = AEApi.instance()
            .storage()
            .createFluidList();
        if (tag == null) {
            return out;
        }
        for (int x = 0; x < tag.tagCount(); x++) {
            final IAEFluidStack ais = AEFluidStack.loadFluidStackFromNBT(tag.getCompoundTagAt(x));
            if (ais != null) {
                out.add(ais);
            }
        }
        return out;
    }

    private IItemList<IAEItemStack> readList(final NBTTagList tag) {
        final IItemList<IAEItemStack> out = AEApi.instance()
            .storage()
            .createItemList();

        if (tag == null) {
            return out;
        }

        for (int x = 0; x < tag.tagCount(); x++) {
            final IAEItemStack ais = AEItemStack.loadItemStackFromNBT(tag.getCompoundTagAt(x));
            if (ais != null) {
                out.add(ais);
            }
        }

        return out;
    }

    @Override
    public NBTBase writeToNBT() {
        if (this.getStackType() == AEItemStackType.ITEM_STACK_TYPE) {
            return writeList(this.getItems());
        } else if (this.getStackType() == AEFluidStackType.FLUID_STACK_TYPE) {
            return writeFluidList(this.getFluids());
        } else {
            return writeOtherList(this.getOthers());
        }
    }

    private NBTTagList writeOtherList(final IItemList<IAEStack<?>> myList) {
        final NBTTagList out = new NBTTagList();

        for (final IAEStack<?> ais : myList) {
            if (ais.getStackSize() > 0) {
                out.appendTag(this.writeOther(ais));
            }
        }

        return out;
    }

    private NBTBase writeOther(IAEStack<?> other) {
        final NBTTagCompound out = new NBTTagCompound();

        if (other != null) {
            other.writeToNBT(out);
        }

        return out;
    }

    private NBTTagList writeFluidList(final IItemList<IAEFluidStack> myList) {
        final NBTTagList out = new NBTTagList();

        for (final IAEFluidStack ais : myList) {
            if (ais.getStackSize() > 0) {
                out.appendTag(this.writeFluid(ais));
            }
        }

        return out;
    }

    private NBTBase writeFluid(IAEFluidStack fluid) {
        final NBTTagCompound out = new NBTTagCompound();

        if (fluid != null) {
            fluid.writeToNBT(out);
        }
        return out;
    }

    private NBTTagList writeList(final IItemList<IAEItemStack> myList) {
        final NBTTagList out = new NBTTagList();

        for (final IAEItemStack ais : myList) {
            if (ais.getStackSize() > 0) {
                out.appendTag(this.writeItem(ais));
            }
        }

        return out;
    }

    private NBTTagCompound writeItem(final IAEItemStack item) {
        final NBTTagCompound out = new NBTTagCompound();

        if (item != null) {
            item.writeToNBT(out);
        }

        return out;
    }
}
