package com.asdflj.ae2thing.nei.object;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.glodblock.github.util.Util;

import codechicken.nei.PositionedStack;

public class OrderStack<T> {

    private T RealStack;
    private int index;
    private ItemStack[] items;

    public static final int ITEM = 1;
    public static final int FLUID = 2;
    public static final int CUSTOM = 3;

    private static final String REPLACEMENTS = "ae2thing:replacements";

    public OrderStack(T stack, int order) {
        if (stack == null || order < 0)
            throw new IllegalArgumentException("Trying to create a null or negative order stack!");
        this.RealStack = stack;
        this.index = order;
    }

    public OrderStack(T stack, int order, ItemStack[] items) {
        this(stack, order);
        if (items.length > 1) {
            this.items = items;
        }
    }

    public void putStack(T stack) {
        this.RealStack = stack;
    }

    public void putStack(ItemStack stack) {
        this.RealStack = (T) stack;
    }

    public void putItems(ItemStack[] items) {
        this.items = items;
    }

    public ItemStack[] getItems() {
        return this.items;
    }

    public final void setIndex(int i) {
        this.index = i;
    }

    public T getStack() {
        return RealStack;
    }

    public int getIndex() {
        return index;
    }

    public static OrderStack<ItemStack> pack(PositionedStack stack, int index) {
        if (stack != null && stack.items != null && stack.items.length > 0) {
            if (Items.feather.getDamage(stack.items[0]) == OreDictionary.WILDCARD_VALUE) {
                ItemStack tmp = stack.items[0].copy();
                tmp.setItemDamage(0);
                return new OrderStack<>(tmp, index);
            } else return new OrderStack<>(stack.items[0].copy(), index);
        }
        return null;
    }

    protected void customNBTWriter(NBTTagCompound buf) {}

    protected T customNBTReader(NBTTagCompound buf) {
        return null;
    }

    public final void writeToNBT(NBTTagCompound buf) {
        if (RealStack instanceof ItemStack itemStack) {
            NBTTagCompound tmp = new NBTTagCompound();
            tmp.setByte("t", (byte) ITEM);
            Util.writeItemStackToNBT(itemStack, tmp);
            if (this.items != null && this.items.length > 1) {
                NBTTagList list = new NBTTagList();
                for (ItemStack replacement : this.items) {
                    if (replacement == null || replacement.getItem() == null) continue;
                    NBTTagCompound entry = new NBTTagCompound();
                    Util.writeItemStackToNBT(replacement, entry);
                    list.appendTag(entry);
                }
                if (list.tagCount() > 1) tmp.setTag(REPLACEMENTS, list);
            }
            buf.setTag(index + ":", tmp);
        } else if (RealStack instanceof FluidStack) {
            NBTTagCompound tmp = new NBTTagCompound();
            tmp.setByte("t", (byte) FLUID);
            ((FluidStack) RealStack).writeToNBT(tmp);
            buf.setTag(index + ":", tmp);
        } else {
            NBTTagCompound tmp = new NBTTagCompound();
            tmp.setByte("t", (byte) CUSTOM);
            customNBTWriter(tmp);
            buf.setTag(index + ":", tmp);
        }
    }

    public static OrderStack<?> readFromNBT(NBTTagCompound buf, @Nullable OrderStack<?> dummy, int index) {
        if (!buf.hasKey(index + ":")) return null;
        NBTTagCompound info = buf.getCompoundTag(index + ":");
        byte id = info.getByte("t");
        switch (id) {
            case ITEM -> {
                final ItemStack realStack = Util.loadItemStackFromNBT(info);
                if (realStack == null) return null;
                final OrderStack<ItemStack> stack = new OrderStack<>(realStack, index);
                if (info.hasKey(REPLACEMENTS)) {
                    final List<ItemStack> replacements = new ArrayList<>();
                    final NBTTagList list = info.getTagList(REPLACEMENTS, 10);
                    for (int x = 0; x < list.tagCount(); x++) {
                        final ItemStack replacement = Util.loadItemStackFromNBT(list.getCompoundTagAt(x));
                        if (replacement != null) replacements.add(replacement);
                    }
                    if (replacements.size() > 1) stack.putItems(replacements.toArray(new ItemStack[0]));
                }
                return stack;
            }
            case FLUID -> {
                return new OrderStack<>(FluidStack.loadFluidStackFromNBT(info), index);
            }
            case CUSTOM -> {
                if (dummy == null) throw new IllegalOrderStackID(id);
                return new OrderStack<>(dummy.customNBTReader(buf), index);
            }
            default -> throw new IllegalOrderStackID(id);
        }
    }
}
