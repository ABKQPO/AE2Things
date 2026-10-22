package com.asdflj.ae2thing.nei.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.nei.NEEUtils;
import com.asdflj.ae2thing.nei.NEIStackSelector;
import com.asdflj.ae2thing.nei.object.IRecipeExtractor;
import com.asdflj.ae2thing.nei.object.IRecipeExtractorLegacy;
import com.asdflj.ae2thing.nei.object.OrderStack;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;

public final class Recipe {

    private static final HashMap<String, IRecipeExtractor> IdentifierMap = new HashMap<>();
    private static final HashMap<String, IRecipeExtractor> IdentifierMapLegacy = new HashMap<>();

    public static void addRecipeMap(String recipeIdentifier, IRecipeExtractor extractor) {
        IdentifierMap.put(recipeIdentifier, extractor);
        if (recipeIdentifier == null && extractor instanceof IRecipeExtractorLegacy legacy) {
            IdentifierMapLegacy.put(legacy.getClassName(), extractor);
        }
    }

    public static List<OrderStack<?>> getPackageInputs(IRecipeHandler recipe, int index, boolean priority) {
        final List<OrderStack<?>> out = collectInputs(recipe, index);
        return priority ? prioritizeFluids(out) : out;
    }

    public static List<OrderStack<?>> getPackageOutputs(IRecipeHandler recipe, int index, boolean useOther) {
        return collectOutputs(recipe, index, useOther);
    }

    public static List<String> getSupportRecipes() {
        return new ArrayList<>(IdentifierMap.keySet());
    }

    private static List<OrderStack<?>> collectInputs(IRecipeHandler recipe, int index) {
        if (!(recipe instanceof TemplateRecipeHandler tRecipe)) return Collections.emptyList();

        final String identifier = tRecipe.getOverlayIdentifier();
        if (!IdentifierMap.containsKey(identifier)) {
            final List<OrderStack<?>> NEEInputs = collectNEEInputs(tRecipe, index, identifier);
            if (NEEInputs != null) return NEEInputs;
            return getDefaultPackageInputs(tRecipe, index);
        }
        if (identifier == null) return getPackageInputsLegacy(recipe, index);
        final IRecipeExtractor extractor = IdentifierMap.get(identifier);
        if (extractor == null) return Collections.emptyList();
        return extractor.getInputIngredients(new ArrayList<>(tRecipe.getIngredientStacks(index)), recipe, index);
    }

    private static List<OrderStack<?>> collectOutputs(IRecipeHandler recipe, int index, boolean useOther) {
        if (!(recipe instanceof TemplateRecipeHandler tRecipe)) return Collections.emptyList();

        final String identifier = tRecipe.getOverlayIdentifier();
        if (!IdentifierMap.containsKey(identifier)) {
            final List<OrderStack<?>> NEEOutputs = collectNEEOutputs(tRecipe, index, identifier);
            if (NEEOutputs != null) return NEEOutputs;
            return getDefaultPackageOutputs(tRecipe, index);
        }
        if (identifier == null) return getPackageOutputsLegacy(recipe, index, useOther);
        final IRecipeExtractor extractor = IdentifierMap.get(identifier);
        if (extractor == null) return Collections.emptyList();
        final List<PositionedStack> tmp = new ArrayList<>();
        final PositionedStack result = tRecipe.getResultStack(index);
        if (result != null) tmp.add(result);
        if (useOther) tmp.addAll(tRecipe.getOtherStacks(index));
        return extractor.getOutputIngredients(tmp, recipe, index);
    }

    private static List<OrderStack<?>> collectNEEInputs(TemplateRecipeHandler recipe, int index, String identifier) {
        if (identifier == null || !Mods.NOT_ENOUGH_ENERGISTICS.isModLoaded()) return null;
        final NEEUtils.Extraction extraction = NEEUtils.extract(recipe, index, identifier);
        if (extraction.unMatched()) return null;
        return pack(extraction.inputs(), identifier, false);
    }

    private static List<OrderStack<?>> collectNEEOutputs(TemplateRecipeHandler recipe, int index, String identifier) {
        if (identifier == null || !Mods.NOT_ENOUGH_ENERGISTICS.isModLoaded()) return null;
        final NEEUtils.Extraction extraction = NEEUtils.extract(recipe, index, identifier);
        if (extraction.unMatched()) return null;
        return pack(extraction.outputs(), identifier, true);
    }

    private static List<OrderStack<?>> getDefaultPackageInputs(TemplateRecipeHandler tRecipe, int index) {
        try {
            return pack(tRecipe.getIngredientStacks(index), NEEUtils.realIdentifierOf(tRecipe), false);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static List<OrderStack<?>> getDefaultPackageOutputs(TemplateRecipeHandler tRecipe, int index) {
        try {
            final List<PositionedStack> stacks = new ArrayList<>();
            final PositionedStack result = tRecipe.getResultStack(index);
            if (result != null) stacks.add(result);
            stacks.addAll(tRecipe.getOtherStacks(index));
            return pack(stacks, NEEUtils.realIdentifierOf(tRecipe), true);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static List<OrderStack<?>> pack(List<PositionedStack> stacks, String identifier, boolean output) {
        final List<OrderStack<?>> out = new ArrayList<>(stacks.size());
        int index = 0;
        for (PositionedStack ps : stacks) {
            if (ps == null) continue;
            final OrderStack<?> orderStack = NEIStackSelector.pack(ps, index, identifier, output);
            if (orderStack == null) continue;
            out.add(orderStack);
            index++;
        }
        return out;
    }

    private static List<OrderStack<?>> prioritizeFluids(List<OrderStack<?>> out) {
        final List<OrderStack<?>> reordered = new ArrayList<>(out.size());
        int index = 0;
        for (OrderStack<?> orderStack : out) {
            if (orderStack != null && orderStack.getStack() instanceof FluidStack) {
                reordered.add(new OrderStack<>(orderStack.getStack(), index++));
            }
        }
        for (OrderStack<?> orderStack : out) {
            if (orderStack != null && orderStack.getStack() instanceof ItemStack) {
                reordered.add(new OrderStack<>(orderStack.getStack(), index++));
            }
        }
        for (OrderStack<?> orderStack : out) {
            if (orderStack != null && !(orderStack.getStack() instanceof FluidStack)
                && !(orderStack.getStack() instanceof ItemStack)) {
                reordered.add(new OrderStack<>(orderStack.getStack(), index++));
            }
        }
        return reordered;
    }

    public static List<OrderStack<?>> getPackageInputsLegacy(IRecipeHandler recipe, int index) {
        final IRecipeExtractor extractor = legacyExtractor(recipe);
        if (extractor == null) return Collections.emptyList();
        return extractor.getInputIngredients(new ArrayList<>(recipe.getIngredientStacks(index)), recipe, index);
    }

    public static List<OrderStack<?>> getPackageOutputsLegacy(IRecipeHandler recipe, int index, boolean useOther) {
        final IRecipeExtractor extractor = legacyExtractor(recipe);
        if (extractor == null) return Collections.emptyList();
        final List<PositionedStack> tmp = new ArrayList<>();
        final PositionedStack result = recipe.getResultStack(index);
        if (result != null) tmp.add(result);
        if (useOther) tmp.addAll(recipe.getOtherStacks(index));
        return extractor.getOutputIngredients(tmp, recipe, index);
    }

    private static IRecipeExtractor legacyExtractor(IRecipeHandler recipe) {
        if (recipe == null) return null;
        return IdentifierMapLegacy.get(
            recipe.getClass()
                .getName());
    }
}
