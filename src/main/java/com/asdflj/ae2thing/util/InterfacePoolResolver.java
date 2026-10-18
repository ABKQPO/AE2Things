package com.asdflj.ae2thing.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.integration.Mods;

import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.RecipeCatalysts;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.RecipeMapWorkable;
import gregtech.api.recipe.RecipeCategory;
import gregtech.api.recipe.RecipeMap;
import gregtech.common.blocks.ItemMachines;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

@SideOnly(Side.CLIENT)
public final class InterfacePoolResolver {

    public enum PoolState {
        MATCH,
        NO_MATCH,
        UNKNOWN
    }

    public record PoolInfo(Set<RecipeMap<?>> maps, Set<String> catalystIds) {

        public boolean hasAnyPool() {
            return !this.maps.isEmpty() || !this.catalystIds.isEmpty();
        }
    }

    private static final PoolInfo NO_POOL = new PoolInfo(Collections.emptySet(), Collections.emptySet());
    private static final Long2ObjectMap<PoolInfo> CACHE = new Long2ObjectOpenHashMap<>();

    private InterfacePoolResolver() {}

    public static void invalidateCaches() {
        CACHE.clear();
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

    private static long keyOf(ItemStack rep) {
        final int itemId = Item.getIdFromItem(rep.getItem()) & 0xFFFF;
        final int damage = rep.getItemDamage() & 0xFFFF;
        final int tagHash = rep.getTagCompound() == null ? 0
            : rep.getTagCompound()
                .hashCode();
        return ((long) itemId << 48) | ((long) damage << 32) | (tagHash & 0xFFFFFFFFL);
    }

    private static PoolInfo computePoolInfo(ItemStack rep) {
        final Set<RecipeMap<?>> maps = new ObjectOpenHashSet<>();
        final Set<String> catalystIds = new ObjectOpenHashSet<>();
        if (Mods.hasGTRecipeMapWorkable()) {
            try {
                final IMetaTileEntity metaTileEntity = ItemMachines.getMetaTileEntity(rep);
                if (metaTileEntity instanceof RecipeMapWorkable workable) {
                    final Collection<RecipeMap<?>> available = workable.getAvailableRecipeMaps();
                    for (RecipeMap<?> map : available) {
                        if (map != null) {
                            maps.add(map);
                        }
                    }
                }
            } catch (Throwable ignored) {}
            try {
                for (RecipeCategory category : RecipeCategory.ALL_RECIPE_CATEGORIES.values()) {
                    if (RecipeCatalysts.containsCatalyst(category.unlocalizedName, rep)) {
                        catalystIds.add(category.unlocalizedName);
                    }
                }
            } catch (Throwable ignored) {}
        }
        return maps.isEmpty() && catalystIds.isEmpty() ? NO_POOL : new PoolInfo(maps, catalystIds);
    }

    public static PoolState classify(ItemStack rep, List<RecipeCategory> categories, List<RecipeMap<?>> maps,
        List<String> overlayIds) {
        final PoolInfo info = poolInfoOf(rep);
        if (info.hasAnyPool()) {
            for (RecipeMap<?> map : maps) {
                if (info.maps()
                    .contains(map)) {
                    return PoolState.MATCH;
                }
            }
            for (RecipeCategory category : categories) {
                if (info.catalystIds()
                    .contains(category.unlocalizedName)) {
                    return PoolState.MATCH;
                }
            }
        }
        for (String id : overlayIds) {
            if (isCatalyst(id, rep)) {
                return PoolState.MATCH;
            }
        }
        return info.hasAnyPool() ? PoolState.NO_MATCH : PoolState.UNKNOWN;
    }

    public static PoolState classifyForRecipe(ItemStack rep, IRecipeHandler recipe) {
        if (rep == null || recipe == null) {
            return PoolState.UNKNOWN;
        }
        try {
            if (RecipeCatalysts.containsCatalyst(recipe, rep)) {
                return PoolState.MATCH;
            }
        } catch (Throwable ignored) {}
        return poolInfoOf(rep).hasAnyPool() ? PoolState.NO_MATCH : PoolState.UNKNOWN;
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
