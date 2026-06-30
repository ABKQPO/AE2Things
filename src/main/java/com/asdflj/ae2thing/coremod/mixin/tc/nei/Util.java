package com.asdflj.ae2thing.coremod.mixin.tc.nei;

import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.common.item.ItemPhial;
import com.gtnewhorizons.aspectrecipeindex.ModItems;
import com.gtnewhorizons.aspectrecipeindex.common.items.ItemAspect;

import thaumcraft.api.aspects.Aspect;

public class Util {

    public static ItemStack itemPhial2ItemAspect(ItemStack item) {
        if (item != null && item.getItem() instanceof ItemPhial) {
            ItemStack result = new ItemStack(ModItems.itemAspect);
            Aspect aspect = ItemPhial.getAspect(item);
            if (aspect != null) {
                ItemAspect.setAspect(result, aspect);
                return result;
            }
        }
        return item;
    }
}
