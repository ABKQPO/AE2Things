package com.asdflj.ae2thing.util;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.common.integration.tc.EssentiaItemContainerHelper;
import thaumicenergistics.common.storage.AEEssentiaStack;
import thaumicenergistics.common.storage.AEEssentiaStackType;

/**
 * Compatibility helper that replaces the removed AE2FluidCraft AspectUtil. In newer ThaumicEnergistics essentia is no
 * longer modeled as a Forge fluid; it is the IAEStack type {@link AEEssentiaStack}. This helper centralizes the
 * aspect/essentia conversion and container queries against the new API so the rest of the mod depends on a single
 * AE2Things owned entry point.
 */
public final class AspectUtil {

    /**
     * Amount of essentia represented by a single item form unit (phial). Replaces the old fixed AspectUtil.R ratio.
     */
    public static final int R = AEEssentiaStackType.ESSENTIA_STACK_TYPE.getAmountPerUnit();

    public static final EssentiaItemContainerHelper HELPER = EssentiaItemContainerHelper.INSTANCE;

    private AspectUtil() {}

    public static boolean isEssentiaContainer(ItemStack is) {
        return is != null && AEEssentiaStackType.ESSENTIA_STACK_TYPE.isContainerItemForType(is);
    }

    public static boolean isEmptyEssentiaContainer(ItemStack is) {
        return is != null && HELPER.isContainerEmpty(is);
    }

    @Nullable
    public static Aspect getAspectFromJar(ItemStack is) {
        if (is == null) return null;
        return HELPER.getAspectInContainer(is);
    }

    @Nullable
    public static AEEssentiaStack getEssentiaFromContainer(ItemStack is) {
        if (is == null) return null;
        return AEEssentiaStackType.ESSENTIA_STACK_TYPE.getStackFromContainerItem(is);
    }

    public static AEEssentiaStack newEssentiaStack(Aspect aspect, long amount) {
        return new AEEssentiaStack(aspect, amount);
    }

    /**
     * The essentia ME monitor backing a network, or null when the grid has no storage cache.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static IMEMonitor<AEEssentiaStack> getEssentiaMonitor(IGrid grid) {
        if (grid == null) return null;
        IStorageGrid storageGrid = grid.getCache(IStorageGrid.class);
        if (storageGrid == null) return null;
        return (IMEMonitor<AEEssentiaStack>) storageGrid.getMEMonitor(AEEssentiaStackType.ESSENTIA_STACK_TYPE);
    }

    /**
     * Render an aspect amount in a GUI slot using the essentia stack's own renderer.
     */
    public static void drawAspect(EntityPlayer player, int x, int y, Aspect aspect, long amount) {
        if (aspect == null) return;
        new AEEssentiaStack(aspect, amount <= 0 ? 1 : amount).drawInGui(Minecraft.getMinecraft(), x, y);
    }
}
