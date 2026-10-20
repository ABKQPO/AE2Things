package com.asdflj.ae2thing.util.enhancedsearch;

import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.RecipeCatalysts;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@SideOnly(Side.CLIENT)
public final class EnhancedSearch {

    public record Query(boolean active, boolean invalid, String invalidReason, List<String> poolIds,
        List<String> nameHints, List<String> tokens) {

        public Query {
            poolIds = poolIds == null ? Collections.emptyList() : poolIds;
            nameHints = nameHints == null ? Collections.emptyList() : nameHints;
            tokens = tokens == null ? Collections.emptyList() : tokens;
        }

        public static Query inactive() {
            return new Query(false, false, null, null, null, null);
        }

        public static Query invalid(String reason) {
            return new Query(false, true, reason, null, null, null);
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
            return switch (InterfacePoolResolver.classify(rep, this.poolIds)) {
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

    private EnhancedSearch() {}

    public static void invalidate() {
        cached = null;
        cachedText = null;
    }

    public static Query parse(String text, boolean enabled) {
        final String query = text == null ? "" : text;
        if (cached != null && cachedEnabled == enabled && query.equals(cachedText)) return cached;
        final Query parsed = parseQuery(query, enabled);
        cachedEnabled = enabled;
        cachedText = query;
        cached = parsed;
        return cached;
    }

    public static String poolQuery(IRecipeHandler recipe) {
        final String identifier;
        try {
            identifier = RecipeCatalysts.getRecipeID(recipe);
        } catch (Throwable ignored) {
            return null;
        }
        if (identifier == null || identifier.trim()
            .isEmpty()) return null;
        final String query = "@" + identifier.trim();
        try {
            if (parse(query, true).isInvalid()) return null;
        } catch (Throwable ignored) {
            return null;
        }
        return query;
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

    public static boolean looksLikeQuery(String text) {
        if (text == null) return false;
        final String trimmed = text.trim();
        return trimmed.length() > 1 && trimmed.charAt(0) == '@' && !trimmed.startsWith("@@");
    }

    public static String describe(Query query) {
        if (query == null || !query.isActive()
            || query.poolIds()
                .isEmpty())
            return null;
        return "@" + String.join(" | ", query.poolIds());
    }

    public static String tooltipExtra(Query query, String rawText, boolean enabled, boolean debug) {
        final StringBuilder tooltip = new StringBuilder();
        final String text = rawText == null ? "" : rawText.trim();
        if (!text.isEmpty()) {
            tooltip.append('\n')
                .append(StatCollector.translateToLocalFormatted("ae2thing.interface_terminal.search.current", text));
        }
        if (!enabled && looksLikeQuery(text)) {
            tooltip.append('\n')
                .append(StatCollector.translateToLocal("ae2thing.interface_terminal.search.disabled"));
        }
        final String pool = debug ? describe(query) : null;
        if (pool != null) {
            tooltip.append('\n')
                .append(StatCollector.translateToLocalFormatted("ae2thing.interface_terminal.search.pool", pool));
        }
        return tooltip.toString();
    }

    private static Query parseQuery(String query, boolean enabled) {
        if (!enabled) return Query.inactive();
        if (!query.startsWith("@") || query.startsWith("@@")) return Query.inactive();

        int headEnd = query.indexOf(' ', 1);
        if (headEnd < 0) headEnd = query.length();
        final String head = query.substring(1, headEnd)
            .trim();
        if (head.isEmpty()) return Query.inactive();

        final List<String> poolIds = RecipeSearchAlias.resolve(head);
        if (poolIds.isEmpty()) {
            final List<String> suggestions = RecipeSearchAlias.suggestions(head, 5);
            return Query.invalid(
                suggestions.isEmpty()
                    ? StatCollector
                        .translateToLocalFormatted("ae2thing.interface_terminal.search.invalid.category_only", head)
                    : StatCollector.translateToLocalFormatted(
                        "ae2thing.interface_terminal.search.invalid.category_hint",
                        head,
                        String.join(", ", suggestions)));
        }

        final List<String> hints = new ObjectArrayList<>();
        addHint(hints, head);
        for (String identifier : poolIds) {
            addHint(hints, identifier);
            addHint(hints, RecipeSearchAlias.titleOf(identifier));
        }

        return new Query(
            true,
            false,
            null,
            poolIds,
            hints,
            SearchTokens.split(
                query.substring(headEnd)
                    .toLowerCase()));
    }

    private static void addHint(List<String> hints, String hint) {
        if (hint == null) return;
        final String lower = hint.trim()
            .toLowerCase();
        if (lower.isEmpty() || hints.contains(lower)) return;
        hints.add(lower);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
