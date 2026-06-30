package com.asdflj.ae2thing.coremod.mixin.tc.nei;

import static com.asdflj.ae2thing.coremod.mixin.tc.nei.Util.itemPhial2ItemAspect;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.gtnewhorizons.aspectrecipeindex.nei.AlchemyRecipeHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.AspectCombinationHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.InfusionRecipeHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.arcaneworkbench.ShapedArcaneRecipeHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.arcaneworkbench.ShapelessArcaneRecipeHandler;

@Mixin(
    value = { AspectCombinationHandler.class, ShapedArcaneRecipeHandler.class, ShapelessArcaneRecipeHandler.class,
        AlchemyRecipeHandler.class, InfusionRecipeHandler.class })
public class MixinTemplateRecipeHandler {

    @ModifyVariable(method = "loadCraftingRecipes*", at = @At("HEAD"), remap = false, ordinal = 0, argsOnly = true)
    public ItemStack ae2thing$loadCraftingRecipes(ItemStack ingredient) {
        return itemPhial2ItemAspect(ingredient);
    }

    @ModifyVariable(method = "loadUsageRecipes", at = @At("HEAD"), remap = false, ordinal = 0, argsOnly = true)
    public ItemStack ae2thing$loadUsageRecipes(ItemStack ingredient) {
        return itemPhial2ItemAspect(ingredient);
    }

}
