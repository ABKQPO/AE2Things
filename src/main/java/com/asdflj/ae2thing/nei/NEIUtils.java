package com.asdflj.ae2thing.nei;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.asdflj.ae2thing.nei.object.OrderStack;

import gregtech.api.util.GTOreDictUnificator;

public class NEIUtils {

    public static List<OrderStack<?>> compress(List<OrderStack<?>> list) {
        List<OrderStack<?>> comp = new LinkedList<>();
        for (OrderStack<?> orderStack : list) {
            if (orderStack == null) continue;
            if (orderStack.getStack() instanceof FluidStack) {
                comp.add(orderStack);
                continue;
            }
            ItemStack currentStack = (ItemStack) orderStack.getStack();
            if (currentStack.stackSize == 0) {
                comp.add(orderStack);
                continue;
            }
            boolean find = false;
            for (OrderStack<?> storedStack : comp) {
                if (storedStack == null || !(storedStack.getStack() instanceof ItemStack firstStack)) continue;
                boolean areItemStackEqual = firstStack.isItemEqual(currentStack)
                    && ItemStack.areItemStackTagsEqual(firstStack, currentStack);
                if (areItemStackEqual
                    && (firstStack.stackSize + currentStack.stackSize) <= firstStack.getMaxStackSize()) {
                    find = true;
                    ((ItemStack) storedStack.getStack()).stackSize = firstStack.stackSize + currentStack.stackSize;
                }
            }
            if (!find) {
                comp.add(orderStack);
            }
        }
        return comp.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public static List<OrderStack<?>> clearNull(List<OrderStack<?>> list) {
        AtomicInteger i = new AtomicInteger(0);
        return list.stream()
            .filter(Objects::nonNull)
            .filter(
                orderStack -> !(orderStack.getStack() != null && orderStack.getStack() instanceof ItemStack
                    && ((ItemStack) orderStack.getStack()).stackSize == 0))
            .peek(orderStack -> orderStack.setIndex(i.getAndIncrement()))
            .collect(Collectors.toList());
    }

    public static List<OrderStack<?>> replaceHotIngot(List<OrderStack<?>> outputs) {
        if (outputs == null || outputs.isEmpty()) return outputs;

        List<OrderStack<?>> result = new ArrayList<>(outputs.size());
        for (OrderStack<?> src : outputs) {
            if (src == null || !(src.getStack() instanceof ItemStack is)) {
                result.add(src);
                continue;
            }

            for (int id : OreDictionary.getOreIDs(is)) {
                String oreName = OreDictionary.getOreName(id);
                if (oreName == null || !oreName.startsWith("ingotHot")) continue;

                String coldName = "ingot" + oreName.substring("ingotHot".length());
                List<ItemStack> candidates = OreDictionary.getOres(coldName);
                if (candidates == null || candidates.isEmpty()) continue;

                // noinspection SequencedCollectionMethodCanBeUsed
                ItemStack cold = candidates.get(0);
                if (cold == null) continue;

                ItemStack out = cold.copy();
                try {
                    ItemStack unified = GTOreDictUnificator.setStack(out);
                    if (unified != null) out = unified;
                } catch (Throwable ignored) {}

                out.stackSize = is.stackSize;
                if (is.hasTagCompound()) {
                    out.setTagCompound(
                        (NBTTagCompound) is.getTagCompound()
                            .copy());
                }
                is = out;
            }

            result.add(new OrderStack<>(is, src.getIndex()));
        }

        return result;
    }

    public static List<OrderStack<?>> multiply(List<OrderStack<?>> list, int multiplier) {
        if (list == null || list.isEmpty() || multiplier <= 1) return list;
        final List<OrderStack<?>> out = new ArrayList<>(list.size());
        for (OrderStack<?> src : list) {
            if (src == null) continue;
            if (src.getStack() instanceof ItemStack is) {
                final ItemStack copy = is.copy();
                if (copy.stackSize > 0) {
                    final long scaled = (long) copy.stackSize * multiplier;
                    copy.stackSize = (int) Math.min(Integer.MAX_VALUE, scaled);
                }

                final OrderStack<?> copyOS = new OrderStack<>(copy, src.getIndex());
                if (src.getItems() != null && src.getItems().length > 1) {
                    copyOS.putItems(src.getItems());
                }
                out.add(copyOS);
            } else if (src.getStack() instanceof FluidStack fs) {
                final FluidStack copy = fs.copy();
                if (copy.amount > 0) {
                    final long scaled = (long) copy.amount * multiplier;
                    copy.amount = (int) Math.min(Integer.MAX_VALUE, scaled);
                }
                out.add(new OrderStack<>(copy, src.getIndex()));
            } else {
                out.add(src);
            }
        }

        return out;
    }

    public static List<OrderStack<?>> prepareNonConsumedInputs(List<OrderStack<?>> list, boolean includeNonConsumed) {
        if (list == null || list.isEmpty()) return list;
        final List<OrderStack<?>> out = new ArrayList<>(list.size());
        for (OrderStack<?> orderStack : list) {
            if (orderStack == null) continue;
            if (orderStack.getStack() instanceof ItemStack is && is.stackSize == 0) {
                if (!includeNonConsumed) continue;
                is.stackSize = 1;
            }
            out.add(orderStack);
        }
        return out;
    }
}
