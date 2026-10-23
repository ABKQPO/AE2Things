package com.asdflj.ae2thing.util.enhancedsearch;

import java.util.Collections;
import java.util.Set;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface RecipePools {

    default Set<Object> availableMaps(ItemStack rep) {
        return Collections.emptySet();
    }

    default boolean hasCategory(ItemStack rep) {
        return false;
    }

    default boolean matchesCategory(Set<Object> maps, String id) {
        return false;
    }

    default boolean matchesTransferRect(Set<Object> maps, String id) {
        return false;
    }
}
