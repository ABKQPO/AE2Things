package com.asdflj.ae2thing.coremod.mixin.tc.nei;

import static com.asdflj.ae2thing.coremod.mixin.tc.nei.Util.itemPhial2ItemAspect;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.gtnewhorizons.aspectrecipeindex.nei.ItemsContainingAspectHandler;

import codechicken.nei.recipe.TemplateRecipeHandler;

@Mixin(ItemsContainingAspectHandler.class)
public abstract class MixinLoadCraftingRecipes extends TemplateRecipeHandler {

    @ModifyVariable(method = "loadCraftingRecipes", at = @At("HEAD"), remap = false, ordinal = 0, argsOnly = true)
    public ItemStack ae2thing$loadCraftingRecipes(ItemStack ingredient) {
        return itemPhial2ItemAspect(ingredient);
    }

}
