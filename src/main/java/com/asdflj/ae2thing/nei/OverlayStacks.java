package com.asdflj.ae2thing.nei;

import net.minecraft.client.gui.inventory.GuiContainer;

import com.asdflj.ae2thing.api.AE2ThingAPI;
import com.asdflj.ae2thing.client.gui.widget.IGuiMonitor;
import com.asdflj.ae2thing.util.Ae2ReflectClient;
import com.asdflj.ae2thing.util.Util;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IDisplayRepo;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.AEBaseGui;
import appeng.client.me.ItemRepo;
import codechicken.nei.PositionedStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class OverlayStacks {

    public record Presence(boolean found, boolean stored, boolean craftable) {}

    private OverlayStacks() {}

    public static IItemList<IAEStack<?>> listFor(GuiContainer gui) {
        if (gui == null) return null;
        if (gui instanceof IGuiMonitor gm) {
            return Ae2ReflectClient.getList(gm.getRepo());
        }
        if (AE2ThingAPI.instance()
            .terminal()
            .isTerminal(gui)) {
            IDisplayRepo repo = Util.getDisplayRepo((AEBaseGui) gui);
            if (repo instanceof ItemRepo itemRepo) {
                return Ae2ReflectClient.getList(itemRepo);
            }
        }
        return null;
    }

    public static Presence presence(IItemList<IAEStack<?>> list, PositionedStack stack) {
        if (list == null || stack == null || stack.item == null) return new Presence(false, false, false);
        final IAEStack<?> item = Util.toAEStack(stack.item);
        if (item == null) return new Presence(false, false, false);
        final IAEStack<?> found = list.findPrecise(item);
        if (found == null) return new Presence(false, false, false);
        return new Presence(true, found.getStackSize() > 0, found.isCraftable());
    }
}
