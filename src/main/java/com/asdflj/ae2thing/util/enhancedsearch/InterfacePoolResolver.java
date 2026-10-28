package com.asdflj.ae2thing.util.enhancedsearch;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import codechicken.nei.recipe.RecipeCatalysts;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

@SideOnly(Side.CLIENT)
public final class InterfacePoolResolver {

    public enum PoolState {
        MATCH,
        NO_MATCH,
        UNKNOWN
    }

    public record PoolInfo(Set<Object> maps, boolean hasCategory) {

        public boolean hasAnyPool() {
            return !this.maps.isEmpty() || this.hasCategory;
        }
    }

    private static final PoolInfo NO_POOL = new PoolInfo(Collections.emptySet(), false);
    private static final Long2ObjectMap<PoolInfo> CACHE = new Long2ObjectOpenHashMap<>();

    private InterfacePoolResolver() {}

    public static void invalidateCaches() {
        CACHE.clear();
        GTRecipeUtil.invalidate();
    }

    public static PoolInfo poolInfoOf(ItemStack rep) {
        if (rep == null || rep.getItem() == null) {
            return NO_POOL;
        }
        final long key = keyOf(rep);
        final PoolInfo cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        final PoolInfo info = computePoolInfo(rep);
        CACHE.put(key, info);
        return info;
    }

    public static PoolState classify(ItemStack rep, String activeMapId, List<String> poolIds) {
        final PoolInfo info = poolInfoOf(rep);
        if (activeMapId != null && info.hasAnyPool()) {
            final Boolean verdict = GTRecipeUtil.mapVerdict(activeMapId, info.maps(), poolIds);
            if (verdict != null) {
                return verdict ? PoolState.MATCH : PoolState.NO_MATCH;
            }
        }
        return classifyByMachine(info, rep, poolIds);
    }

    public static PoolState classifyByMachine(ItemStack rep, List<String> poolIds) {
        return classifyByMachine(poolInfoOf(rep), rep, poolIds);
    }

    private static PoolState classifyByMachine(PoolInfo info, ItemStack rep, List<String> poolIds) {
        for (String id : poolIds) {
            if (isCatalyst(id, rep)) return PoolState.MATCH;
            if (GTRecipeUtil.matchesCategory(info.maps(), id)) return PoolState.MATCH;
            if (GTRecipeUtil.matchesTransferRect(info.maps(), id)) return PoolState.MATCH;
        }
        return info.hasAnyPool() ? PoolState.NO_MATCH : PoolState.UNKNOWN;
    }

    public static boolean hasCatalysts(String id) {
        if (id == null) return false;
        try {
            return !RecipeCatalysts.getRecipeCatalysts(id)
                .isEmpty();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static long keyOf(ItemStack rep) {
        final int itemId = Item.getIdFromItem(rep.getItem()) & 0xFFFF;
        final int damage = rep.getItemDamage() & 0xFFFF;
        final int tagHash = rep.getTagCompound() == null ? 0
            : rep.getTagCompound()
                .hashCode();
        return ((long) itemId << 48) | ((long) damage << 32) | (tagHash & 0xFFFFFFFFL);
    }

    private static PoolInfo computePoolInfo(ItemStack rep) {
        final Set<Object> maps = GTRecipeUtil.availableMaps(rep);
        final boolean hasCategory = GTRecipeUtil.hasCategory(rep);
        if (maps.isEmpty() && !hasCategory) return NO_POOL;
        return new PoolInfo(maps, hasCategory);
    }

    private static boolean isCatalyst(String id, ItemStack rep) {
        if (id == null || rep == null) {
            return false;
        }
        try {
            return RecipeCatalysts.containsCatalyst(id, rep);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
