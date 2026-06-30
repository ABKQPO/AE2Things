package com.asdflj.ae2thing.coremod;

import javax.annotation.Nonnull;

import com.asdflj.ae2thing.integration.Mods;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    AE_CLIENT(new MixinBuilder()
        .addClientMixins(
            "ae.AccessorGuiScrollbar",
            "ae.MixinAEBaseGui",
            "ae.MixinContainerCraftConfirm",
            "ae.MixinCraftingCPUCluster",
            "ae.MixinGuiCraftAmount",
            "ae.MixinGuiCraftConfirm",
            "ae.MixinItemRepo",
            "ae.MixinTileIOPort",
            "ae.MixinContainerCraftAmount")
        .addRequiredMod(Mods.AE2)
        .setPhase(Phase.LATE)),

    AE_SERVER(new MixinBuilder()
        .addCommonMixins(
            "ae.MixinContainerCraftConfirm",
            "ae.MixinCraftingCPUCluster",
            "ae.MixinTileIOPort",
            "ae.MixinContainerCraftAmount")
        .addRequiredMod(Mods.AE2)
        .setPhase(Phase.LATE)),

    ASPECT_RECIPE_INDEX(new MixinBuilder()
        .addClientMixins("tc.nei.MixinLoadCraftingRecipes", "tc.nei.MixinTemplateRecipeHandler")
        .addRequiredMod(Mods.ASPECT_RECIPE_INDEX)
        .setPhase(Phase.LATE)),

    BLOCK_RENDERER(new MixinBuilder()
        .addClientMixins("br.MixinBRUtil")
        .addRequiredMod(Mods.BLOCK_RENDERER)
        .setPhase(Phase.LATE)),

    NEI(new MixinBuilder()
        .addClientMixins(
            "nei.MixinGuiContainerManager",
            "nei.MixinGuiOverlayButton",
            "nei.MixinIOverlayHandler",
            "nei.MixinPanelWidget",
            "nei.MixinRecipeItemInputHandler")
        .addRequiredMod(Mods.NOT_ENOUGH_ITEMS)
        .setPhase(Phase.LATE)),

    WIRELESS_CRAFTING_TERMINAL(new MixinBuilder()
        .addCommonMixins("wct.MixinRandomUtils")
        .addRequiredMod(Mods.WIRELESS_CRAFTING_TERMINAL)
        .setPhase(Phase.LATE));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
