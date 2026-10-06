package com.asdflj.ae2thing.nei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraftforge.common.MinecraftForge;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.Tags;
import com.asdflj.ae2thing.client.gui.GuiCraftingTerminal;
import com.asdflj.ae2thing.client.gui.GuiInfusionPatternTerminal;
import com.asdflj.ae2thing.client.gui.GuiWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.nei.recipes.FluidRecipe;
import com.github.vfyjxf.nee.nei.NEETerminalBookmarkContainerHandler;
import com.github.vfyjxf.nee.processor.IRecipeProcessor;
import com.github.vfyjxf.nee.processor.RecipeProcessor;

import codechicken.lib.config.ConfigTagParent;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.event.NEIConfigsLoadedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

@SuppressWarnings("unused")
public class NEI_TH_Config implements IConfigureNEI {

    private static final ConfigTagParent tag = NEIClientConfig.global.config;
    private static boolean registered = false;

    @Override
    public void loadConfig() {
        API.registerNEIGuiHandler(new AE2TH_NEIGuiHandler());
        List<String> recipes = new ArrayList<>();
        recipes.add("crafting");
        recipes.add("crafting2x2");
        for (String identifier : recipes) {
            // that NEE handlers take priority
            if (!API.hasGuiOverlayHandler(GuiCraftingTerminal.class, identifier)) {
                API.registerGuiOverlayHandler(GuiCraftingTerminal.class, CraftingTransferHandler.INSTANCE, identifier);
            }
        }
        recipes.clear();
        recipes.add("infusionCrafting");
        recipes.add("cruciblerecipe");
        for (String identifier : recipes) {
            if (!API.hasGuiOverlayHandler(GuiInfusionPatternTerminal.class, identifier)) {
                API.registerGuiOverlayHandler(
                    GuiInfusionPatternTerminal.class,
                    PatternTerminalRecipeTransferHandler.INSTANCE,
                    identifier);
            }
        }
        for (String identifier : FluidRecipe.getSupportRecipes()) {
            if (!API.hasGuiOverlayHandler(GuiWirelessDualInterfaceTerminal.class, identifier)) {
                API.registerGuiOverlayHandler(
                    GuiWirelessDualInterfaceTerminal.class,
                    PatternTerminalRecipeTransferHandler.INSTANCE,
                    identifier);
            }
        }
        API.addOption(new BaseToggleButton(ButtonConstants.HISTORY, false));
        API.addOption(new BaseToggleButton(ButtonConstants.INVENTORY_STATE));
        API.addOption(new BaseToggleButton(ButtonConstants.ULTRA_TERMINAL_MODE));
        API.addOption(new BaseToggleButton(ButtonConstants.DUAL_INTERFACE_TERMINAL, false));
        API.addOption(new BaseToggleButton(ButtonConstants.DUAL_INTERFACE_TERMINAL_APPEND_CIRCUIT_DAMAGE));
        // API.addOption(new BaseToggleButton(ButtonConstants.PINNED_BAR)); //remove
        // API.addOption(new BaseToggleButton(ButtonConstants.PINNED_BAR_REMOVE));
        // API.addOption(new BaseToggleButton(ButtonConstants.PINNED_BAR_CRAFTING_STATE));
        API.addOption(new BaseToggleButton(ButtonConstants.NEI_CRAFT_ITEM));
        if (Mods.PROGRAMMABLE_HATCHES.isModLoaded()) {
            API.addOption(new BaseToggleButton(ButtonConstants.DUAL_INTERFACE_TERMINAL_FILL_CIRCUIT, false));
        }
        if (Mods.BLOCK_RENDERER.isModLoaded()) {
            API.addOption(new BaseToggleButton(ButtonConstants.BLOCK_RENDER));
        }
        if (Mods.NOT_ENOUGH_ENERGISTICS.isModLoaded()) {
            API.registerBookmarkContainerHandler(
                GuiWirelessDualInterfaceTerminal.class,
                NEETerminalBookmarkContainerHandler.instance);

            if (!registered) {
                MinecraftForge.EVENT_BUS.register(this);
                registered = true;
            }
        }
    }

    public static boolean getConfigValue(String identifier) {
        return tag.getTag(identifier)
            .getBooleanValue(true);
    }

    @Override
    public String getName() {
        return AE2Thing.NAME;
    }

    @Override
    public String getVersion() {
        return Tags.VERSION;
    }

    @SubscribeEvent
    public void installNeeRecipeProcessor(NEIConfigsLoadedEvent event) {
        Set<String> defaultIdentifiers = new HashSet<>(
            Arrays.asList("crafting", "crafting2x2", "brewing", "smelting", "fuel", null));
        Set<String> identifiers = new HashSet<>(defaultIdentifiers);

        RecipeProcessor.recipeProcessors.stream()
            .map(IRecipeProcessor::getAllOverlayIdentifier)
            .forEach(identifiers::addAll);

        for (String identifier : identifiers) {
            if (!API.hasGuiOverlayHandler(GuiWirelessDualInterfaceTerminal.class, identifier)) {
                API.registerGuiOverlay(GuiWirelessDualInterfaceTerminal.class, identifier);
                API.registerGuiOverlayHandler(
                    GuiWirelessDualInterfaceTerminal.class,
                    PatternTerminalRecipeTransferHandler.INSTANCE,
                    identifier);
            }
        }
    }
}
