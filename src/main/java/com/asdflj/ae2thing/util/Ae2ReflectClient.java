package com.asdflj.ae2thing.util;

import static com.glodblock.github.util.Ae2Reflect.readField;
import static com.glodblock.github.util.Ae2Reflect.reflectField;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.glodblock.github.client.gui.GuiFluidInterface;
import com.glodblock.github.client.gui.container.ContainerFluidInterface;
import com.glodblock.github.inventory.IDualHost;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiCraftingStatus;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.me.ItemRepo;
import codechicken.nei.SearchField;
import codechicken.nei.util.TextHistory;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class Ae2ReflectClient {

    private static final Field fSearchField_history;
    private static final Field fTextHistory_history;
    private static final Field fItemRepo_view;
    private static final Field fItemRepo_list;
    private static final Field fGuiFluidInterface_cont;

    static {
        try {
            fItemRepo_view = reflectField(ItemRepo.class, "view");
            fItemRepo_list = reflectField(ItemRepo.class, "list");
            fGuiFluidInterface_cont = reflectField(GuiFluidInterface.class, "cont");
            fSearchField_history = reflectField(SearchField.class, "history");
            fTextHistory_history = reflectField(TextHistory.class, "history");
        } catch (NoSuchFieldException | SecurityException e) {
            throw new IllegalStateException("Failed to initialize AE2 reflection hacks!", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Slot> getInventorySlots(AEBaseGui gui) {
        return com.glodblock.github.util.Ae2ReflectClient.getInventorySlots(gui);
    }

    public static void rewriteIcon(GuiCraftingStatus gui, ItemStack icon) {
        com.glodblock.github.util.Ae2ReflectClient.rewriteIcon(gui, icon);
    }

    public static GuiTabButton getOriginalGuiButton(GuiCraftingStatus gui) {
        return com.glodblock.github.util.Ae2ReflectClient.getOriginalGuiButton(gui);
    }

    public static Set<Slot> getDragClick(AEBaseGui gui) {
        return com.glodblock.github.util.Ae2ReflectClient.getDragClick(gui);
    }

    public static TextHistory getHistory(SearchField searchField) {
        return readField(searchField, fSearchField_history);
    }

    public static List<String> getHistoryList(TextHistory textHistory) {
        return readField(textHistory, fTextHistory_history);
    }

    public static ArrayList<IAEStack<?>> getView(ItemRepo repo) {
        return readField(repo, fItemRepo_view);
    }

    public static IItemList<IAEStack<?>> getList(ItemRepo repo) {
        return readField(repo, fItemRepo_list);
    }

    public static IDualHost getHost(GuiFluidInterface gui) {
        ContainerFluidInterface container = readField(gui, fGuiFluidInterface_cont);
        return container == null ? null : container.getTile();
    }

}
