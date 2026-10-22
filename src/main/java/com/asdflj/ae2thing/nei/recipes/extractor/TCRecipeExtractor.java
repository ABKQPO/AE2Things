package com.asdflj.ae2thing.nei.recipes.extractor;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.common.item.ItemPhial;
import com.asdflj.ae2thing.nei.NEEUtils;
import com.asdflj.ae2thing.nei.NEIStackSelector;
import com.asdflj.ae2thing.nei.object.IRecipeExtractor;
import com.asdflj.ae2thing.nei.object.OrderStack;
import com.gtnewhorizons.aspectrecipeindex.common.items.ItemAspect;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import thaumcraft.api.aspects.Aspect;

public class TCRecipeExtractor implements IRecipeExtractor {

    private final boolean c;

    public TCRecipeExtractor(boolean isCraft) {
        c = isCraft;
    }

    @Override
    public List<OrderStack<?>> getInputIngredients(List<PositionedStack> rawInputs) {
        return convert(rawInputs, NEEUtils.NULL_IDENTIFIER, false);
    }

    @Override
    public List<OrderStack<?>> getOutputIngredients(List<PositionedStack> rawOutputs) {
        return convert(rawOutputs, NEEUtils.NULL_IDENTIFIER, true);
    }

    @Override
    public List<OrderStack<?>> getInputIngredients(List<PositionedStack> rawInputs, IRecipeHandler recipe, int index) {
        return convert(rawInputs, NEEUtils.realIdentifierOf(recipe), false);
    }

    @Override
    public List<OrderStack<?>> getOutputIngredients(List<PositionedStack> rawOutputs, IRecipeHandler recipe,
        int index) {
        return convert(rawOutputs, NEEUtils.realIdentifierOf(recipe), true);
    }

    private List<OrderStack<?>> convert(List<PositionedStack> raw, String identifier, boolean output) {
        List<OrderStack<?>> tmp = new LinkedList<>();
        for (int i = 0; i < raw.size(); i++) {
            final PositionedStack positionedStack = raw.get(i);
            if (positionedStack == null) continue;
            final int index;
            if (c) {
                final int col = (positionedStack.relx - 25) / 18;
                final int row = (positionedStack.rely - 6) / 18;
                index = col + row * 3;
            } else {
                index = i;
            }
            final OrderStack<?> stack = NEIStackSelector.pack(positionedStack, index, identifier, output);
            if (stack == null) continue;
            if (stack.getStack() instanceof ItemStack is && is.getItem() instanceof ItemAspect) {
                final Aspect aspect = ItemAspect.getAspect(is);
                if (aspect != null) {
                    stack.putStack(ItemPhial.newStack(aspect, is.stackSize));
                }
            }
            tmp.add(stack);
        }
        return tmp;
    }
}
