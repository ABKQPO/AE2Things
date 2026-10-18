package com.asdflj.ae2thing.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.nei.object.OrderStack;

import reobf.proghatches.item.ItemProgrammingCircuit;
import reobf.proghatches.item.ItemProgrammingToolkit;

public class PHUtil {

    public static List<OrderStack<?>> transfer(List<OrderStack<?>> inputs) {
        AtomicBoolean circuit = new AtomicBoolean(false);
        if (!ItemProgrammingToolkit.holding()) {
            return inputs;
        }
        AtomicInteger i = new AtomicInteger(0);
        ArrayList<OrderStack<?>> spec = new ArrayList<>();
        List<OrderStack<?>> ret = inputs.stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingInt(OrderStack::getIndex))
            .filter(orderStack -> {
                if (orderStack.getStack() != null && orderStack.getStack() instanceof ItemStack is
                    && is.stackSize == 0) {
                    circuit.set(true);
                    spec.add(new OrderStack<>(ItemProgrammingCircuit.wrap(is), orderStack.getIndex()));
                    return false;
                }

                return true;
            })
            .toList();

        if (!circuit.get() && ItemProgrammingToolkit.addEmptyProgCiruit()) {
            spec.addFirst(new OrderStack<>(ItemProgrammingCircuit.wrap(null), 0));
        }

        spec.addAll(ret);
        spec.forEach((orderStack -> orderStack.setIndex(i.getAndIncrement())));

        return spec;
    }

    public static boolean isProgrammingToolkitEnabled() {
        return ItemProgrammingToolkit.holding() && ItemProgrammingToolkit.mode != 0;
    }
}
