package com.asdflj.ae2thing.util.enhancedsearch;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.integration.Mods;

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
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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

    private static Object2ObjectOpenHashMap<String, RecipeCategory> categories;
    private static Object2ObjectOpenHashMap<String, Set<RecipeMap<?>>> transferRectMaps;

    private InterfacePoolResolver() {}

    public static void invalidateCaches() {
        CACHE.clear();
        categories = null;
        transferRectMaps = null;
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

    public static PoolState classify(ItemStack rep, List<String> poolIds) {
        final PoolInfo info = poolInfoOf(rep);
        for (String id : poolIds) {
            if (isCatalyst(id, rep)) return PoolState.MATCH;
            final RecipeCategory category = categoryById(id);
            if (category != null && info.maps()
                .contains(category.recipeMap)) {
                return PoolState.MATCH;
            }
            for (RecipeMap<?> map : mapsByTransferRect(id)) {
                if (info.maps()
                    .contains(map)) return PoolState.MATCH;
            }
        }
        return info.hasAnyPool() ? PoolState.NO_MATCH : PoolState.UNKNOWN;
    }

    public static RecipeCategory categoryById(String id) {
        if (id == null) return null;
        return categories().get(id);
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

    private static Object2ObjectOpenHashMap<String, RecipeCategory> categories() {
        if (categories != null) return categories;
        final Object2ObjectOpenHashMap<String, RecipeCategory> result = new Object2ObjectOpenHashMap<>();
        try {
            for (RecipeCategory category : RecipeCategory.ALL_RECIPE_CATEGORIES.values()) {
                result.put(category.unlocalizedName, category);
            }
        } catch (Throwable ignored) {}
        categories = result;
        return categories;
    }

    private static Set<RecipeMap<?>> mapsByTransferRect(String id) {
        if (id == null) return Collections.emptySet();
        return transferRectMaps().getOrDefault(id, Collections.emptySet());
    }

    private static Object2ObjectOpenHashMap<String, Set<RecipeMap<?>>> transferRectMaps() {
        if (transferRectMaps != null) return transferRectMaps;
        final Object2ObjectOpenHashMap<String, Set<RecipeMap<?>>> result = new Object2ObjectOpenHashMap<>();
        try {
            for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
                if (map == null) continue;
                final String transferId = map.getFrontend()
                    .getUIProperties().neiTransferRectId;
                if (transferId == null || transferId.isEmpty()) continue;
                result.computeIfAbsent(transferId, _ -> new ObjectOpenHashSet<>())
                    .add(map);
            }
        } catch (Throwable ignored) {}
        transferRectMaps = result;
        return transferRectMaps;
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
