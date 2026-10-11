package com.asdflj.ae2thing.nei;

import static com.asdflj.ae2thing.nei.NEI_TH_Config.getConfigValue;
import static com.asdflj.ae2thing.proxy.ClientProxy.mouseHandlers;
import static net.minecraft.client.gui.GuiScreen.isShiftKeyDown;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.api.Constants;
import com.asdflj.ae2thing.client.gui.GuiInfusionPatternTerminal;
import com.asdflj.ae2thing.client.gui.GuiWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.client.gui.widget.DualPatternSlot;
import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.nei.object.OrderStack;
import com.asdflj.ae2thing.nei.recipes.FluidRecipe;
import com.asdflj.ae2thing.network.CPacketTransferRecipe;
import com.asdflj.ae2thing.proxy.ClientProxy;
import com.asdflj.ae2thing.util.GTUtil;
import com.asdflj.ae2thing.util.PHUtil;
import com.google.common.base.Optional;

import appeng.api.AEApi;
import appeng.client.gui.AEBaseGui;
import appeng.container.slot.SlotFake;
import appeng.util.Platform;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.StackInfo;
import codechicken.nei.recipe.TemplateRecipeHandler;

public class PatternTerminalRecipeTransferHandler implements IOverlayHandler {

    public static final PatternTerminalRecipeTransferHandler INSTANCE = new PatternTerminalRecipeTransferHandler();

    public static final HashSet<String> notOtherSet = new HashSet<>();
    public static final HashSet<String> craftSet = new HashSet<>();

    static {
        notOtherSet.add("smelting");
        notOtherSet.add("brewing");
        craftSet.add("crafting");
        craftSet.add("crafting2x2");
    }

    private static Object findSameIngredient(ItemStack[] items, ItemStack item, FluidStack fluid,
        Constants.MouseWheel wheel) {
        for (int i = 0; i < items.length; i++) {
            int index = i + wheel.direction;
            ItemStack picked = items[index < 0 ? items.length - 1 : index % items.length];
            if (fluid != null) {
                FluidStack fluidInfo = StackInfo.getFluid(items[i]);
                if (fluidInfo == null || !fluid.isFluidEqual(fluidInfo)) continue;
                return StackInfo.getFluid(picked);
            } else if (Platform.isSameItemPrecise(item, items[i])) {
                return picked;
            }
        }
        return null;
    }

    public PatternTerminalRecipeTransferHandler() {
        mouseHandlers.add((event, overlayButton) -> {
            GuiScreen screen = Minecraft.getMinecraft().currentScreen;
            if (screen instanceof AEBaseGui g && overlayButton != null && GuiScreen.isShiftKeyDown()) {
                GuiOverlayButton btn = ClientProxy.getOverlayButton();
                if (btn == null) return false;
                ItemStack slotItem = null;
                if (g.theSlot instanceof SlotFake slot) {
                    slotItem = slot.getStack();
                } else if (g instanceof GuiWirelessDualInterfaceTerminal gui
                    && gui.getVirtualMESlotUnderMouse() instanceof DualPatternSlot virtualSlot
                    && virtualSlot.getAEStack() != null) {
                        slotItem = virtualSlot.getAEStack()
                            .getItemStackForNEI();
                    }
                if (slotItem != null) {
                    final FluidStack slotFluid = StackInfo.getFluid(slotItem);
                    List<PositionedStack> list = btn.handlerRef.handler.getIngredientStacks(btn.handlerRef.recipeIndex);
                    for (PositionedStack stack : list) {
                        Object result = findSameIngredient(
                            stack.items,
                            slotItem,
                            slotFluid,
                            event.scrollAmount == -1 ? Constants.MouseWheel.NEXT : Constants.MouseWheel.PREVIEW);
                        if (result != null) {
                            List<OrderStack<?>> in = new ArrayList<>();
                            List<OrderStack<?>> out = new ArrayList<>();
                            if (result instanceof FluidStack fluidOut) {
                                in.add(new OrderStack<>(slotFluid, 0));
                                out.add(new OrderStack<>(fluidOut, 0));
                            } else if (result instanceof ItemStack itemOut) {
                                in.add(new OrderStack<>(slotItem, 0));
                                out.add(new OrderStack<>(itemOut, 0));
                            }
                            AE2Thing.proxy.netHandler.sendToServer(
                                new CPacketTransferRecipe(
                                    in,
                                    out,
                                    shouldCraft(btn.handlerRef.handler),
                                    isShiftKeyDown(),
                                    Constants.NEI_MOUSE_WHEEL));
                            return true;
                        }
                    }
                }
            }
            return false;
        });
    }

    @Override
    public void overlayRecipe(GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex, boolean shift) {
        if (firstGui instanceof GuiInfusionPatternTerminal) {
            List<OrderStack<?>> in = FluidRecipe.getPackageInputs(recipe, recipeIndex, false);
            List<OrderStack<?>> out = FluidRecipe.getPackageOutputs(recipe, recipeIndex, false);
            AE2Thing.proxy.netHandler.sendToServer(new CPacketTransferRecipe(out, in, true, shift));
        } else if (firstGui instanceof GuiWirelessDualInterfaceTerminal) {
            boolean priority = ((GuiWirelessDualInterfaceTerminal) firstGui).container.prioritize;
            boolean craft = shouldCraft(recipe);
            List<OrderStack<?>> in;
            in = FluidRecipe.getPackageInputs(recipe, recipeIndex, !craft && priority);
            setSuggestion(craft, recipe, (GuiWirelessDualInterfaceTerminal) firstGui, in);
            if (Mods.PROGRAMMABLE_HATCHES.isModLoaded() && !craft) {
                in = PHUtil.transfer(in);
            }
            List<OrderStack<?>> out = FluidRecipe.getPackageOutputs(recipe, recipeIndex, !notUseOther(recipe));
            AE2Thing.proxy.netHandler.sendToServer(new CPacketTransferRecipe(in, out, craft, shift));
        }
    }

    private void setSuggestion(boolean craft, IRecipeHandler recipe, GuiWirelessDualInterfaceTerminal gui,
        List<OrderStack<?>> in) {
        String suggestion;
        if (craft) {
            Optional<ItemStack> molecular = AEApi.instance()
                .definitions()
                .blocks()
                .molecularAssembler()
                .maybeStack(1);
            if (molecular.isPresent()) {
                suggestion = Platform.getItemDisplayName(molecular.get());
            } else {
                suggestion = "";
            }
        } else if (Mods.isGt5UnofficialLoaded() || Mods.isLegacyGt5Loaded()) {
            suggestion = GTUtil.getRecipeName(recipe, in);
        } else {
            suggestion = recipe.getRecipeName();
        }
        if (getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL)) {
            gui.setSearchFieldText(suggestion);
            gui.setHighlightSlot();
        } else {
            gui.setSearchFieldSuggestion(suggestion);
        }

        if (getConfigValue(ButtonConstants.DUAL_INTERFACE_TERMINAL_AUTO_UPLOAD)) {
            gui.setSearchFieldText(suggestion);
            if (!gui.upload(recipe, craft)) {
                gui.setHighlightSlot();
            }
        }
    }

    private boolean notUseOther(IRecipeHandler recipeHandler) {
        TemplateRecipeHandler tRecipe = (TemplateRecipeHandler) recipeHandler;
        return notOtherSet.contains(tRecipe.getOverlayIdentifier());
    }

    private boolean shouldCraft(IRecipeHandler recipeHandler) {
        TemplateRecipeHandler tRecipe = (TemplateRecipeHandler) recipeHandler;
        return craftSet.contains(tRecipe.getOverlayIdentifier());
    }

}
