package com.asdflj.ae2thing.api.adapter.pattern;

import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

public class DualInterfacePatternValidators {

    private static final CopyOnWriteArrayList<IDualInterfacePatternValidator> VALIDATORS = new CopyOnWriteArrayList<>();

    public static void register(IDualInterfacePatternValidator validator) {
        if (validator != null) {
            VALIDATORS.addIfAbsent(validator);
        }
    }

    public static void unregister(IDualInterfacePatternValidator validator) {
        if (validator != null) {
            VALIDATORS.remove(validator);
        }
    }

    public static boolean accepts(ItemStack pattern, DualInterfacePatternTarget target, EntityPlayerMP player) {
        if (pattern == null || target == null || target.patterns() == null || target.world() == null) {
            return false;
        }
        for (IDualInterfacePatternValidator validator : VALIDATORS) {
            try {
                if (validator.accepts(pattern, target, player)) {
                    return true;
                }
            } catch (RuntimeException ignored) {}
        }
        return false;
    }
}
