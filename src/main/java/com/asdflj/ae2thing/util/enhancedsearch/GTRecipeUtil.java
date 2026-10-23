package com.asdflj.ae2thing.util.enhancedsearch;

import java.util.Collection;
import java.util.Set;

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
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

@SideOnly(Side.CLIENT)
public final class GTRecipeUtil {

    private GTRecipeUtil() {}

    public static void invalidate() {
        try {
            Backend.invalidateCache();
        } catch (Throwable ignored) {}
    }

    public static String baseMapId(String pageId) {
        return PagesHolder.INSTANCE.baseMapId(pageId);
    }

    public static Set<Object> availableMaps(ItemStack rep) {
        return PoolsHolder.INSTANCE.availableMaps(rep);
    }

    public static boolean hasCategory(ItemStack rep) {
        return PoolsHolder.INSTANCE.hasCategory(rep);
    }

    public static boolean matchesCategory(Set<Object> maps, String id) {
        return PoolsHolder.INSTANCE.matchesCategory(maps, id);
    }

    public static boolean matchesTransferRect(Set<Object> maps, String id) {
        return PoolsHolder.INSTANCE.matchesTransferRect(maps, id);
    }

    private record PagesHolder() {

        private static final RecipePages INSTANCE = create();

        private static RecipePages create() {
            try {
                return new Backend();
            } catch (Throwable ignored) {
                return new RecipePages() {};
            }
        }
    }

    private record PoolsHolder() {

        private static final RecipePools INSTANCE = create();

        private static RecipePools create() {
            try {
                return new Backend();
            } catch (Throwable ignored) {
                return new RecipePools() {};
            }
        }
    }

    private static final class Backend implements RecipePages, RecipePools {

        private static Object2ObjectOpenHashMap<String, RecipeCategory> categories;
        private static Object2ObjectOpenHashMap<String, Set<Object>> transferRectMaps;

        private static void invalidateCache() {
            categories = null;
            transferRectMaps = null;
        }

        @Override
        public String baseMapId(String pageId) {
            if (pageId == null || pageId.isEmpty()) return RecipePages.super.baseMapId(pageId);
            try {
                final RecipeCategory category = categoryById(pageId);
                if (category == null) {
                    return RecipePages.super.baseMapId(pageId);
                }
                final String base = category.recipeMap.unlocalizedName;
                if (base.isEmpty() || base.equals(pageId)) {
                    return RecipePages.super.baseMapId(pageId);
                }
                return base;
            } catch (Throwable ignored) {
                return RecipePages.super.baseMapId(pageId);
            }
        }

        @Override
        public Set<Object> availableMaps(ItemStack rep) {
            if (rep == null || rep.getItem() == null || !Mods.hasGTRecipeMapWorkable()) {
                return RecipePools.super.availableMaps(rep);
            }
            try {
                final Set<Object> maps = new ObjectOpenHashSet<>();
                final IMetaTileEntity metaTileEntity = ItemMachines.getMetaTileEntity(rep);
                if (metaTileEntity instanceof RecipeMapWorkable workable) {
                    final Collection<RecipeMap<?>> available = workable.getAvailableRecipeMaps();
                    for (RecipeMap<?> map : available) {
                        if (map != null) maps.add(map);
                    }
                }
                return maps;
            } catch (Throwable ignored) {
                return RecipePools.super.availableMaps(rep);
            }
        }

        @Override
        public boolean hasCategory(ItemStack rep) {
            if (rep == null || rep.getItem() == null || !Mods.isGt5UnofficialLoaded()) {
                return RecipePools.super.hasCategory(rep);
            }
            try {
                for (RecipeCategory category : RecipeCategory.ALL_RECIPE_CATEGORIES.values()) {
                    if (category == null) continue;
                    if (RecipeCatalysts.containsCatalyst(category.unlocalizedName, rep)) return true;
                }
            } catch (Throwable ignored) {
                return RecipePools.super.hasCategory(rep);
            }
            return RecipePools.super.hasCategory(rep);
        }

        @Override
        public boolean matchesCategory(Set<Object> maps, String id) {
            if (maps == null || maps.isEmpty() || id == null || !Mods.isGt5UnofficialLoaded()) {
                return RecipePools.super.matchesCategory(maps, id);
            }
            try {
                final RecipeCategory category = categoryById(id);
                return category != null && maps.contains(category.recipeMap);
            } catch (Throwable ignored) {
                return RecipePools.super.matchesCategory(maps, id);
            }
        }

        @Override
        public boolean matchesTransferRect(Set<Object> maps, String id) {
            if (maps == null || maps.isEmpty() || id == null || !Mods.isGt5UnofficialLoaded()) {
                return RecipePools.super.matchesTransferRect(maps, id);
            }
            try {
                final Set<Object> candidates = transferRectMaps().get(id);
                if (candidates == null) return RecipePools.super.matchesTransferRect(maps, id);
                for (Object candidate : candidates) {
                    if (maps.contains(candidate)) return true;
                }
            } catch (Throwable ignored) {
                return RecipePools.super.matchesTransferRect(maps, id);
            }
            return RecipePools.super.matchesTransferRect(maps, id);
        }

        private static RecipeCategory categoryById(String id) {
            final Object2ObjectOpenHashMap<String, RecipeCategory> cached = categories();
            return cached == null ? null : cached.get(id);
        }

        private static Object2ObjectOpenHashMap<String, RecipeCategory> categories() {
            if (categories != null) return categories;
            final Object2ObjectOpenHashMap<String, RecipeCategory> result = new Object2ObjectOpenHashMap<>();
            try {
                for (RecipeCategory category : RecipeCategory.ALL_RECIPE_CATEGORIES.values()) {
                    if (category == null) continue;
                    result.put(category.unlocalizedName, category);
                }
            } catch (Throwable ignored) {}
            categories = result;
            return categories;
        }

        private static Object2ObjectOpenHashMap<String, Set<Object>> transferRectMaps() {
            if (transferRectMaps != null) return transferRectMaps;
            final Object2ObjectOpenHashMap<String, Set<Object>> result = new Object2ObjectOpenHashMap<>();
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
    }
}
