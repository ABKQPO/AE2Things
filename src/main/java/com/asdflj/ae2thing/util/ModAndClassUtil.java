package com.asdflj.ae2thing.util;

import java.lang.reflect.Field;

import com.asdflj.ae2thing.integration.Mods;

import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.core.localization.ButtonToolTips;

public class ModAndClassUtil {

    public static boolean NEE = false;
    public static boolean GT5NH = false;
    public static boolean GT5 = false;
    public static boolean NEI = false;
    public static boolean FTR = false;
    public static boolean BACKPACK = false;
    public static boolean ADVENTURE_BACKPACK = false;
    public static boolean HODGEPODGE = false;
    public static boolean THE = false;
    public static boolean WAILA = false;
    public static boolean WCT = false;
    public static boolean IC2 = false;
    public static boolean NECHAR = false;
    public static boolean NECH = false;
    public static boolean BOTANIA = false;
    public static boolean HBM_AE_ADDON = false;
    public static boolean CORE_MOD = false;
    public static boolean TIC = false;
    public static boolean PH = false;
    public static boolean FIND_IT = false;
    public static boolean BLOCK_RENDER = false;
    public static boolean BAUBLES = false;
    public static boolean isTypeFilter;
    public static boolean isCraftStatus;
    public static boolean isDoubleButton;
    public static boolean isBeSubstitutionsButton;

    @SuppressWarnings("all")
    public static void init() {
        isTypeFilter = Mods.hasAe2TypeFilter();
        try {
            Field d = Settings.class.getDeclaredField("CRAFTING_STATUS");
            if (d == null) isCraftStatus = false;
            isCraftStatus = true;
        } catch (NoSuchFieldException e) {
            isCraftStatus = false;
        }
        try {
            Field d = ActionItems.class.getDeclaredField("DOUBLE");
            if (d == null) isDoubleButton = false;
            isDoubleButton = true;
        } catch (NoSuchFieldException e) {
            isDoubleButton = false;
        }
        try {
            Field d = ButtonToolTips.class.getDeclaredField("BeSubstitutionsDescEnabled");
            isBeSubstitutionsButton = true;
        } catch (NoSuchFieldException e) {
            isBeSubstitutionsButton = false;
        }
        GT5NH = Mods.isGt5UnofficialLoaded();
        GT5 = Mods.isLegacyGt5Loaded();
        THE = Mods.THAUMIC_ENERGISTICS.isModLoaded();
        FTR = Mods.FORESTRY.isModLoaded();
        BACKPACK = Mods.BACKPACK.isModLoaded();
        ADVENTURE_BACKPACK = Mods.ADVENTURE_BACKPACK.isModLoaded();
        NEI = Mods.NOT_ENOUGH_ITEMS.isModLoaded();
        HODGEPODGE = Mods.HODGEPODGE.isModLoaded();
        WAILA = Mods.WAILA.isModLoaded();
        IC2 = Mods.IC2.isModLoaded();
        NECHAR = Mods.NECHAR.isModLoaded();
        NECH = Mods.NECH.isModLoaded();
        NEE = Mods.NOT_ENOUGH_ENERGISTICS.isModLoaded();
        BOTANIA = Mods.BOTANIA.isModLoaded();
        CORE_MOD = Mods.CORE_MOD.isModLoaded();
        HBM_AE_ADDON = Mods.HBM_AE_ADDON.isModLoaded();
        TIC = Mods.TINKERS_CONSTRUCT.isModLoaded();
        PH = Mods.PROGRAMMABLE_HATCHES.isModLoaded();
        FIND_IT = Mods.FIND_IT.isModLoaded();
        WCT = Mods.WIRELESS_CRAFTING_TERMINAL.isModLoaded();
        BLOCK_RENDER = Mods.BLOCK_RENDERER.isModLoaded();
        BAUBLES = Mods.BAUBLES.isModLoaded();
    }
}
