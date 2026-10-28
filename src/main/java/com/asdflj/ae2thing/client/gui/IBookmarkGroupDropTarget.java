package com.asdflj.ae2thing.client.gui;

import net.minecraft.item.ItemStack;

public interface IBookmarkGroupDropTarget {

    /**
     * Consumes a NEI bookmark group that was dragged onto this screen.
     *
     * @return the amount of stacks that could be placed, or {@code -1} when the position is not a drop target at all
     *         (the caller then treats the drop as "not handled").
     */
    int handleBookmarkGroupDrop(int mouseX, int mouseY, Iterable<ItemStack> stacks);
}
