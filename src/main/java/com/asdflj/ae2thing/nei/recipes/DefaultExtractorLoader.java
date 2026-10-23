package com.asdflj.ae2thing.nei.recipes;

import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.nei.recipes.extractor.TCRecipeExtractor;
import com.asdflj.ae2thing.nei.recipes.extractor.VanillaRecipeExtractor;

public class DefaultExtractorLoader implements Runnable {

    @Override
    public void run() {
        Recipe.addRecipeMap("smelting", new VanillaRecipeExtractor(false));
        Recipe.addRecipeMap("brewing", new VanillaRecipeExtractor(false));
        Recipe.addRecipeMap("crafting", new VanillaRecipeExtractor(true));
        Recipe.addRecipeMap("crafting2x2", new VanillaRecipeExtractor(true));
        if (Mods.THAUMIC_ENERGISTICS.isModLoaded()) {
            Recipe.addRecipeMap("infusionCrafting", new TCRecipeExtractor(false));
            Recipe.addRecipeMap("cruciblerecipe", new TCRecipeExtractor(false));
        }
    }
}
