package com.asdflj.ae2thing.api.adapter.terminal;

import appeng.client.gui.widgets.GuiImgButton;

/**
 * Lets the AE2Things craft amount screen read the crafting mode button that AE2's {@code GuiCraftAmount} keeps private.
 * The accessor is merged into that class by {@code MixinGuiCraftAmount}.
 */
public interface ICraftingModeHolder {

    GuiImgButton ae2thing$getCraftingMode();
}
