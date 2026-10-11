package com.asdflj.ae2thing.util;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.nei.object.OrderStack;

import codechicken.nei.recipe.IRecipeHandler;
import gregtech.api.interfaces.INonConsumedItemDisplay;
import gregtech.api.metatileentity.CommonBaseMetaTileEntity;
import gregtech.api.recipe.RecipeCategory;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTUtility;
import gregtech.nei.GTNEIDefaultHandler;

public class GTNonConsumedHelper {

    public static Collection<String> collectTokens(IRecipeHandler recipe, List<OrderStack<?>> in, boolean appendCircuit,
        boolean appendNonConsumedItems) {
        Set<String> tokens = new LinkedHashSet<>();
        if (!(recipe instanceof GTNEIDefaultHandler)) return tokens;

        RecipeCategory category = RecipeCategory.ALL_RECIPE_CATEGORIES.get(recipe.getOverlayIdentifier());
        RecipeMap<?> map = category != null ? category.recipeMap : null;

        for (OrderStack<?> order : in) {
            if (!(order.getStack() instanceof ItemStack stack) || stack.stackSize != 0) continue;

            if (appendCircuit && GTUtility.isAnyIntegratedCircuit(stack) && stack.getItemDamage() > 0) {
                tokens.add(String.valueOf(stack.getItemDamage()));
            } else if (appendNonConsumedItems && INonConsumedItemDisplay.isDisplayableItem(map, stack)) {
                tokens.add(CommonBaseMetaTileEntity.getShortItemDisplayName(stack));
            }
        }
        return tokens;
    }
}
