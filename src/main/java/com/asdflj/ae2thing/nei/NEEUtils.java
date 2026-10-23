package com.asdflj.ae2thing.nei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.minecraft.item.ItemStack;

import com.github.vfyjxf.nee.processor.IRecipeProcessor;
import com.github.vfyjxf.nee.processor.RecipeProcessor;
import com.github.vfyjxf.nee.utils.ItemUtils;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;

public final class NEEUtils {

    public static final String NULL_IDENTIFIER = "null";

    private static final Set<String> SKIPPED_PROCESSORS = Set.of("GregTech5", "GregTech6");

    private static IRecipeHandler cachedRecipe;
    private static int cachedIndex = -1;
    private static String cachedIdentifier;
    private static Extraction cachedExtraction = Extraction.MISS;

    private NEEUtils() {}

    public static String realIdentifierOf(IRecipeHandler recipe) {
        if (recipe instanceof TemplateRecipeHandler template) {
            final String identifier = template.getOverlayIdentifier();
            return identifier == null ? NULL_IDENTIFIER : identifier;
        }
        return NULL_IDENTIFIER;
    }

    public static Set<String> allIdentifiers() {
        final Set<String> identifiers = new HashSet<>();
        try {
            for (IRecipeProcessor processor : RecipeProcessor.recipeProcessors) {
                identifiers.addAll(processor.getAllOverlayIdentifier());
            }
        } catch (Throwable ignored) {}
        return identifiers;
    }

    public static Extraction extract(IRecipeHandler recipe, int recipeIndex, String identifier) {
        if (recipe == cachedRecipe && recipeIndex == cachedIndex && Objects.equals(identifier, cachedIdentifier)) {
            return cachedExtraction;
        }
        final Extraction extraction = extractUncached(recipe, recipeIndex, identifier);
        cachedRecipe = recipe;
        cachedIndex = recipeIndex;
        cachedIdentifier = identifier;
        cachedExtraction = extraction;
        return extraction;
    }

    private static Extraction extractUncached(IRecipeHandler recipe, int recipeIndex, String identifier) {
        try {
            for (IRecipeProcessor processor : RecipeProcessor.recipeProcessors) {
                if (SKIPPED_PROCESSORS.contains(processor.getRecipeProcessorId())) continue;
                if (!processor.getAllOverlayIdentifier()
                    .contains(identifier)) continue;
                final List<PositionedStack> inputs = new ArrayList<>(
                    processor.getRecipeInput(recipe, recipeIndex, identifier));
                if (inputs.isEmpty()) continue;
                final List<PositionedStack> outputs = new ArrayList<>(
                    processor.getRecipeOutput(recipe, recipeIndex, identifier));
                if (outputs.isEmpty()) continue;
                return new Extraction(true, inputs, outputs, processor.mergeStacks(recipe, recipeIndex, identifier));
            }
        } catch (Throwable ignored) {}
        return Extraction.MISS;
    }

    public static boolean mergeStacks(IRecipeHandler recipe, int recipeIndex, String identifier) {
        return extract(recipe, recipeIndex, identifier).mergeStacks();
    }

    public static ItemStack preferModItem(ItemStack[] items) {
        if (items == null || items.length == 0) return null;
        try {
            return ItemUtils.getPreferModItem(items);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isPreferItem(ItemStack stack, String identifier) {
        if (stack == null) return false;
        try {
            if (identifier == null) return ItemUtils.isPreferItems(stack);
            return ItemUtils.isPreferItems(stack, "", identifier);
        } catch (Throwable ignored) {
            try {
                return ItemUtils.isPreferItems(stack);
            } catch (Throwable ignored2) {
                return false;
            }
        }
    }

    public static boolean isBlacklisted(ItemStack stack, String identifier) {
        if (stack == null || identifier == null) return false;
        try {
            return ItemUtils.isInBlackList(stack, "", identifier);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public record Extraction(boolean matched, List<PositionedStack> inputs, List<PositionedStack> outputs,
        boolean mergeStacks) {

        private static final Extraction MISS = new Extraction(
            false,
            Collections.emptyList(),
            Collections.emptyList(),
            true);

        public boolean unMatched() {
            return !this.matched;
        }
    }
}
