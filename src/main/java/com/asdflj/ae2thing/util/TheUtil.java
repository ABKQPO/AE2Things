package com.asdflj.ae2thing.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

import com.asdflj.ae2thing.common.item.ItemPhial;
import com.asdflj.ae2thing.common.parts.PartThaumatoriumInterface;
import com.asdflj.ae2thing.common.tile.TileInfusionInterface;
import com.glodblock.github.client.gui.GuiFluidInterface;
import com.glodblock.github.inventory.IDualHost;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.api.gui.IWidgetHost;
import thaumicenergistics.common.items.ItemCraftingAspect;
import thaumicenergistics.common.storage.AEEssentiaStack;

public class TheUtil {

    public static boolean isItemCraftingAspect(IAEItemStack item) {
        return (item.getItem() instanceof ItemCraftingAspect);
    }

    public static boolean isEssentia(AEEssentiaStack essentia) {
        return essentia != null && essentia.getAspect() != null;
    }

    public static AEEssentiaStack itemCraftingAspect2Essentia(IAEItemStack item) {
        Aspect aspect = ItemCraftingAspect.getAspect(item.getItemStack());
        return new AEEssentiaStack(aspect, item.getStackSize());
    }

    public static IAEItemStack itemCraftingAspect2ItemPhial(IAEItemStack item) {
        AEEssentiaStack essentia = itemCraftingAspect2Essentia(item);
        return ItemPhial.newAeStack(essentia);
    }

    public static String getGuiDualInterfaceDisplayName(String displayName, GuiFluidInterface gui) {
        IDualHost host = Ae2ReflectClient.getHost(gui);
        if (host instanceof TileInfusionInterface) {
            return I18n.format(NameConst.GUI_INFUSION_INTERFACE);
        } else if (host instanceof PartThaumatoriumInterface) {
            return I18n.format(NameConst.GUI_PART_THAUMATORIUM_INTERFACE);
        } else {
            return displayName;
        }
    }

    public static IAEItemStack itemPhial2ItemCraftingAspect(IAEItemStack item) {
        Aspect aspect = ItemPhial.getAspect(item.getItemStack());
        IAEItemStack is = AEItemStack.create(ItemCraftingAspect.createStackForAspect(aspect, 1));
        is.setStackSize(item.getStackSize());
        return is;
    }

    @SideOnly(Side.CLIENT)
    public static boolean isTerminal() {
        return Minecraft.getMinecraft().currentScreen instanceof IWidgetHost;
    }

    public static IAEItemStack essentia2CraftingAspect(AEEssentiaStack essentia) {
        if (!isEssentia(essentia)) return null;
        return AEItemStack.create(ItemCraftingAspect.createStackForAspect(essentia.getAspect(), 1));
    }

    public static boolean isSameAspect(IAEItemStack phial, IAEItemStack craftingAspect) {
        if (ItemPhial.isItemPhial(phial) && TheUtil.isItemCraftingAspect(craftingAspect)) {
            Aspect pa = ItemPhial.getAspect(phial.getItemStack());
            Aspect ca = ItemCraftingAspect.getAspect(craftingAspect.getItemStack());
            if (pa == null) return false;
            return pa.getTag()
                .equals(ca.getTag());
        }
        return false;
    }
}
