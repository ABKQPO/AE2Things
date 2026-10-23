package com.asdflj.ae2thing.nei.recipes;

import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.nei.recipes.extractor.TCRecipeExtractor;
import com.asdflj.ae2thing.nei.recipes.extractor.VanillaRecipeExtractor;

public class DefaultExtractorLoader implements Runnable {

    @Override
    public void run() {
        NEIRecipeUtil.addRecipeMap("smelting", new VanillaRecipeExtractor(false));
        NEIRecipeUtil.addRecipeMap("brewing", new VanillaRecipeExtractor(false));
        NEIRecipeUtil.addRecipeMap("crafting", new VanillaRecipeExtractor(true));
        NEIRecipeUtil.addRecipeMap("crafting2x2", new VanillaRecipeExtractor(true));
        if (Mods.THAUMIC_ENERGISTICS.isModLoaded()) {
            NEIRecipeUtil.addRecipeMap("infusionCrafting", new TCRecipeExtractor(false));
            NEIRecipeUtil.addRecipeMap("cruciblerecipe", new TCRecipeExtractor(false));
        }
    }
}
