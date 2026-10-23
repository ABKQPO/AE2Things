package com.asdflj.ae2thing.coremod.mixin.nei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;

import com.asdflj.ae2thing.nei.AEItemOverlayState;
import com.asdflj.ae2thing.nei.OverlayStacks;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.IRecipeHandler;

@Mixin(value = IOverlayHandler.class)
public interface MixinIOverlayHandler extends IOverlayHandler {

    @Override
    default List<GuiOverlayButton.ItemOverlayState> presenceOverlay(GuiContainer firstGui, IRecipeHandler recipe,
        int recipeIndex) {
        final List<GuiOverlayButton.ItemOverlayState> itemPresenceSlots = new ArrayList<>();
        final List<PositionedStack> ingredients = recipe.getIngredientStacks(recipeIndex);
        final IItemList<IAEStack<?>> list = OverlayStacks.listFor(firstGui);
        final List<ItemStack> invStacks = firstGui.inventorySlots.inventorySlots.stream()
            .filter(
                s -> s != null && s.getStack() != null
                    && s.getStack().stackSize > 0
                    && s.isItemValid(s.getStack())
                    && s.canTakeStack(firstGui.mc.thePlayer))
            .map(
                s -> s.getStack()
                    .copy())
            .collect(Collectors.toCollection(ArrayList::new));

        for (PositionedStack stack : ingredients) {
            Optional<ItemStack> used = invStacks.stream()
                .filter(is -> is.stackSize > 0 && stack.contains(is))
                .findAny();
            if (used.isPresent()) {
                ItemStack is = used.get();
                is.stackSize -= 1;
                itemPresenceSlots.add(new GuiOverlayButton.ItemOverlayState(stack, true));
            } else if (list != null) {
                final OverlayStacks.Presence presence = OverlayStacks.presence(list, stack);
                itemPresenceSlots.add(new AEItemOverlayState(stack, presence.found(), presence.craftable()));
            } else {
                itemPresenceSlots.add(new GuiOverlayButton.ItemOverlayState(stack, false));
            }
        }

        return itemPresenceSlots;
    }
}
