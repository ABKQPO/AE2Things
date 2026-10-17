package com.asdflj.ae2thing.util;

import appeng.api.storage.data.IAEStack;
import net.minecraft.item.ItemStack;

import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.common.integration.tc.EssentiaItemContainerHelper;
import thaumicenergistics.common.storage.AEEssentiaStack;
import thaumicenergistics.common.storage.AEEssentiaStackType;

public final class AspectUtil {

    public static final int R = AEEssentiaStackType.ESSENTIA_STACK_TYPE.getAmountPerUnit();

    public static final EssentiaItemContainerHelper HELPER = EssentiaItemContainerHelper.INSTANCE;

    private AspectUtil() {}

    @Nullable
    public static Aspect getAspectFromJar(ItemStack is) {
        if (is == null) return null;
        return HELPER.getAspectInContainer(is);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static IMEMonitor<AEEssentiaStack> getEssentiaMonitor(IGrid grid) {
        if (grid == null) return null;
        IStorageGrid storageGrid = grid.getCache(IStorageGrid.class);
        if (storageGrid == null) return null;
        return (IMEMonitor<AEEssentiaStack>) storageGrid.getMEMonitor(AEEssentiaStackType.ESSENTIA_STACK_TYPE);
    }

    @Nullable
    public static IAEStack<?> toAEStack(ItemStack stack) {
        return AEEssentiaStackType.ESSENTIA_STACK_TYPE.convertStackFromItem(stack);
    }
}
