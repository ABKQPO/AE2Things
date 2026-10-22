package com.asdflj.ae2thing.nei.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.nei.NEEUtils;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.asdflj.ae2thing.nei.object.IRecipeExtractor;
import com.asdflj.ae2thing.nei.object.IRecipeExtractorLegacy;
import com.asdflj.ae2thing.nei.object.OrderStack;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.StackInfo;
import codechicken.nei.recipe.TemplateRecipeHandler;

public final class Recipe {

    private static final HashMap<String, IRecipeExtractor> IdentifierMap = new HashMap<>();
    private static final HashMap<String, IRecipeExtractor> IdentifierMapLegacy = new HashMap<>();

    public static void addRecipeMap(String recipeIdentifier, IRecipeExtractor extractor) {
        IdentifierMap.put(recipeIdentifier, extractor);
        if (recipeIdentifier == null && extractor instanceof IRecipeExtractorLegacy) {
            IdentifierMapLegacy.put(((IRecipeExtractorLegacy) extractor).getClassName(), extractor);
        }
    }

    public static List<OrderStack<?>> getPackageInputs(IRecipeHandler recipe, int index, boolean priority) {
        final List<OrderStack<?>> out = collectInputs(recipe, index);
        return priority ? prioritizeFluids(out) : out;
    }

    private static List<OrderStack<?>> collectInputs(IRecipeHandler recipe, int index) {
        if (!(recipe instanceof TemplateRecipeHandler tRecipe)) return Collections.emptyList();

        final String realIdentifier = NEEUtils.realIdentifierOf(tRecipe);
        if (Mods.NOT_ENOUGH_ENERGISTICS.isModLoaded() && !IdentifierMap.containsKey(realIdentifier) && NEEUtils.find(recipe, index, realIdentifier) != null) {
            return pack(NEEUtils.getInputs(tRecipe, index, realIdentifier), realIdentifier, false);
        }

        if (tRecipe.getOverlayIdentifier() == null) return getPackageInputsLegacy(recipe, index);
        if (IdentifierMap.containsKey(tRecipe.getOverlayIdentifier())) {
            final IRecipeExtractor extractor = IdentifierMap.get(tRecipe.getOverlayIdentifier());
            if (extractor == null) return Collections.emptyList();
            final List<PositionedStack> tmp = new ArrayList<>(tRecipe.getIngredientStacks(index));
            return extractor.getInputIngredients(tmp, recipe, index);
        }

        return getDefaultPackageInputs(tRecipe, index);
    }

    private static List<OrderStack<?>> getDefaultPackageInputs(TemplateRecipeHandler tRecipe, int index) {
        try {
            final List<OrderStack<?>> tmp = new LinkedList<>();
            final AtomicInteger i = new AtomicInteger(0);
            final List<PositionedStack> inputs = tRecipe.getIngredientStacks(index);
            inputs.stream()
                .filter(Objects::nonNull)
                .filter(ps -> ps.item != null)
                .forEach(ps -> tmp.add(unpackFluid(ps.item, i.getAndIncrement(), ps.items)));
            return tmp;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static List<OrderStack<?>> getPackageOutputs(IRecipeHandler recipe, int index, boolean useOther) {
        return collectOutputs(recipe, index, useOther);
    }

    private static List<OrderStack<?>> collectOutputs(IRecipeHandler recipe, int index, boolean useOther) {
        if (!(recipe instanceof TemplateRecipeHandler tRecipe)) return Collections.emptyList();

        final String identifier = NEEUtils.realIdentifierOf(tRecipe);
        if (Mods.NOT_ENOUGH_ENERGISTICS.isModLoaded() && !IdentifierMap.containsKey(identifier)
            && NEEUtils.find(tRecipe, index, identifier) != null) {
            return pack(NEEUtils.getOutputs(tRecipe, index, identifier), identifier, true);
        }

        if (tRecipe.getOverlayIdentifier() == null) return getPackageOutputsLegacy(recipe, index, useOther);
        if (IdentifierMap.containsKey(tRecipe.getOverlayIdentifier())) {
            final IRecipeExtractor extractor = IdentifierMap.get(tRecipe.getOverlayIdentifier());
            if (extractor == null) return Collections.emptyList();
            final List<PositionedStack> tmp = new ArrayList<>(Collections.singleton(tRecipe.getResultStack(index)));
            if (useOther) tmp.addAll(tRecipe.getOtherStacks(index));
            return extractor.getOutputIngredients(tmp, recipe, index);
        }

        return getDefaultPackageOutputs(tRecipe, index);
    }

    private static List<OrderStack<?>> getDefaultPackageOutputs(TemplateRecipeHandler tRecipe, int index) {
        try {
            final List<OrderStack<?>> tmp = new LinkedList<>();
            final AtomicInteger i = new AtomicInteger(0);
            if (tRecipe.getResultStack(index) != null) {
                tmp.add(unpackFluid(tRecipe.getResultStack(index).item, i.getAndIncrement(), null));
            }
            tRecipe.getOtherStacks(index)
                .stream()
                .filter(Objects::nonNull)
                .filter(ps -> ps.item != null)
                .forEach(ps -> tmp.add(unpackFluid(ps.item, i.getAndIncrement(), null)));
            return tmp;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static List<String> getSupportRecipes() {
        return new ArrayList<>(IdentifierMap.keySet());
    }

    public static List<String> getNEESupportRecipes() {
        return new ArrayList<>(NEEUtils.allIdentifiers());
    }

    private static List<OrderStack<?>> prioritizeFluids(List<OrderStack<?>> out) {
        final List<OrderStack<?>> reordered = new ArrayList<>(out.size());
        byte index = 0;
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
        return reordered;
    }

    private static List<OrderStack<?>> pack(List<PositionedStack> stacks, String identifier, boolean output) {
        final List<OrderStack<?>> out = new ArrayList<>(stacks.size());
        int index = 0;
        for (PositionedStack ps : stacks) {
            if (ps == null || ps.item == null) continue;
            final OrderStack<?> orderStack = packOne(ps, index, identifier, output);
            if (orderStack == null) continue;
            out.add(orderStack);
            index++;
        }
        return out;
    }

    private static OrderStack<?> packOne(PositionedStack ps, int index, String identifier, boolean output) {
        final ItemStack selected = NEEUtils.NEIStackSelector.select(ps, identifier, output);
        if (selected == null) return null;
        final FluidStack fluid = StackInfo.getFluid(selected);
        if (fluid != null) return new OrderStack<>(fluid, index);
        return new OrderStack<>(selected, index, ps.items);
    }

    private static OrderStack<?> unpackFluid(ItemStack stack, int index, ItemStack[] items) {
        FluidStack fluid = stack == null ? null : StackInfo.getFluid(stack);
        if (fluid != null) {
            return new OrderStack<>(fluid, index);
        }
        return items == null ? new OrderStack<>(stack, index) : new OrderStack<>(stack, index, items);
    }

    public static List<OrderStack<?>> getPackageInputsLegacy(IRecipeHandler recipe, int index) {
        if (recipe == null || !IdentifierMapLegacy.containsKey(
            recipe.getClass()
                .getName()))
            return Collections.emptyList();
        final IRecipeExtractor extractor = IdentifierMapLegacy.get(
            recipe.getClass()
                .getName());
        if (extractor == null) return Collections.emptyList();
        List<PositionedStack> tmp = new ArrayList<>(recipe.getIngredientStacks(index));
        return extractor.getInputIngredients(tmp, recipe, index);
    }

    public static List<OrderStack<?>> getPackageOutputsLegacy(IRecipeHandler recipe, int index, boolean useOther) {
        if (recipe == null || !IdentifierMapLegacy.containsKey(
            recipe.getClass()
                .getName()))
            return Collections.emptyList();
        final IRecipeExtractor extractor = IdentifierMapLegacy.get(
            recipe.getClass()
                .getName());
        if (extractor == null) return Collections.emptyList();
        List<PositionedStack> tmp = new ArrayList<>(Collections.singleton(recipe.getResultStack(index)));
        if (useOther) tmp.addAll(recipe.getOtherStacks(index));
        return extractor.getOutputIngredients(tmp, recipe, index);
    }
}
