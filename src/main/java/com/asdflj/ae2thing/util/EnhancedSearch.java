package com.asdflj.ae2thing.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import codechicken.nei.recipe.GuiRecipeTab;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.recipe.RecipeCategory;
import gregtech.api.recipe.RecipeMap;

@SideOnly(Side.CLIENT)
public final class EnhancedSearch {

    public record Query(boolean active, boolean invalid, String invalidReason, List<RecipeCategory> categories,
        List<RecipeMap<?>> maps, List<String> overlayIds, List<String> nameHints, List<String> tokens) {

        public Query {
            categories = categories == null ? Collections.emptyList() : categories;
            maps = maps == null ? Collections.emptyList() : maps;
            overlayIds = overlayIds == null ? Collections.emptyList() : overlayIds;
            nameHints = nameHints == null ? Collections.emptyList() : nameHints;
            tokens = tokens == null ? Collections.emptyList() : tokens;
        }

        public static Query inactive() {
            return new Query(false, false, null, null, null, null, null, null);
        }

        public static Query invalid(String reason) {
            return new Query(false, true, reason, null, null, null, null, null);
        }

        public boolean isActive() {
            return this.active;
        }

        public boolean isInvalid() {
            return this.invalid;
        }

        public boolean matches(ItemStack rep, String name, String suffix) {
            if (!this.active) return true;
            final String lowerName = lower(name);
            final String lowerSuffix = lower(suffix);
            if (!this.tokens.isEmpty() && !SearchTokens.matchesAll(this.tokens, lowerName, lowerSuffix)) {
                return false;
            }
            return switch (InterfacePoolResolver.classify(rep, this.categories, this.maps, this.overlayIds)) {
                case MATCH -> true;
                case NO_MATCH -> false;
                case UNKNOWN -> matchesAnyNameHint(this.nameHints, lowerName, lowerSuffix);
            };
        }

        private static boolean matchesAnyNameHint(List<String> hints, String lowerName, String lowerSuffix) {
            for (String hint : hints) {
                if (SearchTokens.matchesAny(hint, lowerName, lowerSuffix)) return true;
            }
            return false;
        }
    }

    private static Query cached;
    private static String cachedText;
    private static boolean cachedEnabled;
    private static Map<String, String> overlayTitles;

    private EnhancedSearch() {}

    public static void invalidate() {
        cached = null;
        cachedText = null;
        overlayTitles = null;
    }

    public static Query parse(String text, boolean enabled) {
        final String query = text == null ? "" : text;
        if (cached != null && cachedEnabled == enabled && query.equals(cachedText)) return cached;
        cachedEnabled = enabled;
        cachedText = query;
        cached = parseQuery(query, enabled);
        return cached;
    }

    public static boolean matchesNameHint(String hint, String name, String suffix) {
        return hint != null && SearchTokens.matchesAny(hint, lower(name), lower(suffix));
    }

    public static String recipeTabName(IRecipeHandler recipe) {
        if (recipe == null) return null;
        try {
            return recipe.getRecipeTabName();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String summary(int visible) {
        return visible == 0 ? StatCollector.translateToLocal("ae2thing.interface_terminal.search.empty")
            : StatCollector.translateToLocalFormatted("ae2thing.interface_terminal.search.hits", visible);
    }

    private static Query parseQuery(String query, boolean enabled) {
        if (!enabled) return Query.inactive();
        if (!query.startsWith("@") || query.startsWith("@@")) return Query.inactive();

        final int close = query.lastIndexOf('@');
        if (close <= 0) {
            return Query.invalid(StatCollector.translateToLocal("ae2thing.interface_terminal.search.invalid.syntax"));
        }
        final String recipeQuery = query.substring(1, close)
            .trim();
        if (recipeQuery.isEmpty()) return Query.inactive();

        final List<RecipeCategory> categories = resolveCategories(recipeQuery);
        List<String> overlayIds = Collections.emptyList();
        if (categories.isEmpty()) {
            overlayIds = resolveOverlayIds(recipeQuery);
            if (overlayIds.isEmpty()) {
                final List<String> suggestions = poolSuggestions(recipeQuery);
                return Query.invalid(
                    suggestions.isEmpty()
                        ? StatCollector.translateToLocalFormatted(
                            "ae2thing.interface_terminal.search.invalid.category_only",
                            recipeQuery)
                        : StatCollector.translateToLocalFormatted(
                            "ae2thing.interface_terminal.search.invalid.category_hint",
                            recipeQuery,
                            String.join(", ", suggestions)));
            }
        }

        final List<RecipeMap<?>> maps = new ArrayList<>();
        for (RecipeCategory category : categories) {
            if (!maps.contains(category.recipeMap)) {
                maps.add(category.recipeMap);
            }
        }

        final List<String> hints = new ArrayList<>();
        addHint(hints, recipeQuery);
        for (RecipeCategory category : categories) {
            addHint(hints, StatCollector.translateToLocal(category.unlocalizedName));
            addHint(hints, StatCollector.translateToLocal(category.recipeMap.unlocalizedName));
        }
        for (String id : overlayIds) {
            addHint(hints, id);
            addHint(hints, titles().get(id));
        }

        return new Query(
            true,
            false,
            null,
            categories,
            maps,
            overlayIds,
            hints,
            SearchTokens.split(
                query.substring(close + 1)
                    .trim()
                    .toLowerCase()));
    }

    private static void addHint(List<String> hints, String hint) {
        if (hint == null) return;
        final String lower = hint.trim()
            .toLowerCase();
        if (lower.isEmpty() || hints.contains(lower)) return;
        hints.add(lower);
    }

    private static List<RecipeCategory> resolveCategories(String recipeQuery) {
        final String lower = recipeQuery.toLowerCase();
        final List<RecipeCategory> exact = new ArrayList<>();
        final List<RecipeCategory> fuzzy = new ArrayList<>();
        for (RecipeCategory category : RecipeCategory.ALL_RECIPE_CATEGORIES.values()) {
            final String localized = StatCollector.translateToLocal(category.unlocalizedName);
            if (category.unlocalizedName.equalsIgnoreCase(recipeQuery) || localized.equalsIgnoreCase(recipeQuery)) {
                exact.add(category);
            } else if (category.unlocalizedName.toLowerCase()
                .contains(lower) || NeCharUtil.INSTANCE.contains(lower, localized.toLowerCase())) {
                    fuzzy.add(category);
                }
        }
        return exact.isEmpty() ? fuzzy : exact;
    }

    private static List<String> resolveOverlayIds(String recipeQuery) {
        final Map<String, String> titles = titles();
        if (titles.isEmpty()) return Collections.emptyList();
        final String lower = recipeQuery.toLowerCase();
        final List<String> exact = new ArrayList<>();
        final List<String> fuzzy = new ArrayList<>();
        for (Map.Entry<String, String> entry : titles.entrySet()) {
            final String id = entry.getKey();
            final String title = entry.getValue();
            if (id.equalsIgnoreCase(recipeQuery) || title.equalsIgnoreCase(recipeQuery)) {
                exact.add(id);
            } else if (id.toLowerCase()
                .contains(lower) || NeCharUtil.INSTANCE.contains(lower, title.toLowerCase())) {
                    fuzzy.add(id);
                }
        }
        return exact.isEmpty() ? fuzzy : exact;
    }

    private static Map<String, String> titles() {
        if (overlayTitles == null) {
            final Map<String, String> titles = new LinkedHashMap<>();
            try {
                for (String id : GuiRecipeTab.handlerMap.keySet()) {
                    String title = id;
                    try {
                        final String translated = GuiRecipeTab.getHandlerTitle(id);
                        if (translated != null && !translated.isEmpty()) title = translated;
                    } catch (Throwable ignored) {}
                    titles.put(id, title);
                }
            } catch (Throwable ignored) {}
            overlayTitles = titles;
        }
        return overlayTitles;
    }

    private static List<String> poolSuggestions(String recipeQuery) {
        final String lower = recipeQuery.toLowerCase();
        final List<String> suggestions = new ArrayList<>();
        for (RecipeCategory category : RecipeCategory.ALL_RECIPE_CATEGORIES.values()) {
            final String localized = StatCollector.translateToLocal(category.unlocalizedName);
            if (NeCharUtil.INSTANCE.contains(lower, category.unlocalizedName.toLowerCase())
                || NeCharUtil.INSTANCE.contains(lower, localized.toLowerCase())) {
                suggestions.add(localized);
                if (suggestions.size() >= 5) return suggestions;
            }
        }
        for (Map.Entry<String, String> entry : titles().entrySet()) {
            final String title = entry.getValue();
            if (NeCharUtil.INSTANCE.contains(
                lower,
                entry.getKey()
                    .toLowerCase())
                || NeCharUtil.INSTANCE.contains(lower, title.toLowerCase())) {
                suggestions.add(title);
                if (suggestions.size() >= 5) return suggestions;
            }
        }
        return suggestions;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
