package com.asdflj.ae2thing.util;

import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.integration.Mods;
import com.science.gtnl.utils.appliedEnergistics.processingPattern.ProcessingPatternCapacity;
import com.science.gtnl.utils.enums.GTNLItemList;

import appeng.util.Platform;

public final class GTNLUtil {

    private static final int DEFAULT_PAGES = 2;
    private static final int MAX_PAGES = 64;
    private static final int LARGE_SIDE = 16;
    private static final int SMALL_SIDE = 4;

    private GTNLUtil() {}

    public static boolean isPatternCapacityEnabled() {
        if (!Mods.SNL.isModLoaded()) return false;
        try {
            return ProcessingPatternCapacity.isEnabled();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static int patternPages() {
        if (!isPatternCapacityEnabled()) return DEFAULT_PAGES;
        try {
            return Math.max(DEFAULT_PAGES, Math.clamp(ProcessingPatternCapacity.pageCount(), 1, MAX_PAGES));
        } catch (Throwable ignored) {
            return DEFAULT_PAGES;
        }
    }

    public static int patternSlotCount() {
        return LARGE_SIDE * patternPages();
    }

    public static int largeSideSlots() {
        return LARGE_SIDE;
    }

    public static int smallSideSlots() {
        return SMALL_SIDE;
    }

    public static boolean isAssemblerMatrix(ItemStack stack) {
        if (stack == null || stack.getItem() == null || !Mods.SNL.isModLoaded()) return false;
        try {
            final ItemStack matrix = GTNLItemList.AssemblerMatrix.get(1);
            return matrix != null && Platform.isSameItemPrecise(matrix, stack);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
