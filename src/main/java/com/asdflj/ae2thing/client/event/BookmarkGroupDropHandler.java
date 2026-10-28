package com.asdflj.ae2thing.client.event;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;

import com.asdflj.ae2thing.client.gui.IBookmarkGroupDropTarget;

import codechicken.nei.ItemPanels;
import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.bookmark.SortableGroup;
import codechicken.nei.guihook.IContainerInputHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BookmarkGroupDropHandler implements IContainerInputHandler {

    public static final BookmarkGroupDropHandler INSTANCE = new BookmarkGroupDropHandler();
    public static final Minecraft MC = Minecraft.getMinecraft();

    private List<ItemStack> draggedGroup;

    @Override
    public boolean keyTyped(GuiContainer gui, char keyChar, int keyCode) {
        return false;
    }

    @Override
    public void onKeyTyped(GuiContainer gui, char keyChar, int keyID) {

    }

    @Override
    public boolean lastKeyTyped(GuiContainer gui, char keyChar, int keyID) {
        return false;
    }

    @Override
    public boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int button) {
        return false;
    }

    @Override
    public void onMouseClicked(GuiContainer gui, int mousex, int mousey, int button) {

    }

    @Override
    public void onMouseUp(GuiContainer gui, int mousex, int mousey, int button) {
        final List<ItemStack> group = this.draggedGroup;
        this.draggedGroup = null;
        if (button != 0 || group == null || !(gui instanceof IBookmarkGroupDropTarget target)) return;

        final int placed = target.handleBookmarkGroupDrop(mousex, mousey, group);
        if (placed < 0) return;

        final int skipped = group.size() - placed;
        if (skipped > 0 && MC.thePlayer != null) {
            MC.thePlayer
                .addChatMessage(new ChatComponentTranslation("ae2thing.chat.universal_bus.config_full", skipped));
        }
    }

    @Override
    public boolean mouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {
        return false;
    }

    @Override
    public void onMouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {

    }

    @Override
    public void onMouseDragged(GuiContainer gui, int mousex, int mousey, int button, long heldTime) {
        if (button != 0 || this.draggedGroup != null) return;
        if (!(gui instanceof IBookmarkGroupDropTarget)) return;

        final SortableGroup group = ItemPanels.bookmarkPanel.sortableGroup;
        if (group == null) return;

        final List<ItemStack> stacks = new ArrayList<>();
        for (BookmarkItem item : group.getBookmarkItems()) {
            final ItemStack stack = item.getItemStack();
            // Group headers / recipe entries have no item stack; dropping null would clear config slots.
            if (stack != null && stack.getItem() != null) {
                stack.stackSize = 1;
                stacks.add(stack);
            }
        }
        if (!stacks.isEmpty()) this.draggedGroup = stacks;
    }
}
