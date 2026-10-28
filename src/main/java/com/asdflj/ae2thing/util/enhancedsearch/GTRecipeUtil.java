package com.asdflj.ae2thing.util.enhancedsearch;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

    public static Boolean mapVerdict(String activeMapId, Set<Object> machineMaps, List<String> poolIds) {
        return PoolsHolder.INSTANCE.mapVerdict(activeMapId, machineMaps, poolIds);
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

        private static boolean isPoolId(String id) {
            if (id == null || id.isEmpty() || !Mods.isGt5UnofficialLoaded()) return false;
            try {
                return categoryById(id) != null || mapById(id) != null || transferRectMaps().containsKey(id);
            } catch (Throwable ignored) {
                return false;
            }
        }

        @Override
        public Boolean mapVerdict(String activeMapId, Set<Object> machineMaps, List<String> poolIds) {
            if (activeMapId == null || activeMapId.isEmpty() || poolIds == null || poolIds.isEmpty()) {
                return null;
            }
            final RecipeMap<?> active = mapById(activeMapId);
            if (active == null) return null;
            boolean owned = false;
            for (String id : poolIds) {
                if (id == null || id.isEmpty() || !isPoolId(id)) continue;
                if (!matchesAny(machineMaps, id)) continue;
                owned = true;
                if (matchesMap(active, id)) return Boolean.TRUE;
            }
            return owned ? Boolean.FALSE : null;
        }

        private boolean matchesAny(Set<Object> maps, String id) {
            if (maps == null || maps.isEmpty()) return false;
            final RecipeMap<?> map = mapById(id);
            if (map != null && maps.contains(map)) return true;
            return matchesCategory(maps, id) || matchesTransferRect(maps, id);
        }

        private static RecipeMap<?> mapById(String id) {
            if (id == null || id.isEmpty() || !Mods.isGt5UnofficialLoaded()) return null;
            try {
                return RecipeMap.ALL_RECIPE_MAPS.get(id);
            } catch (Throwable ignored) {
                return null;
            }
        }

        private boolean matchesMap(RecipeMap<?> map, String id) {
            if (map == null || id == null) return false;
            final String mapId = map.unlocalizedName;
            if (mapId == null || mapId.isEmpty()) return false;
            if (mapId.equals(id)) return true;
            if (mapId.equals(baseMapId(id))) return true;
            return matchesTransferRect(Collections.<Object>singleton(map), id);
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
