package com.asdflj.ae2thing.integration;

import java.util.Locale;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhlib.util.data.IMod;
import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.core.localization.ButtonToolTips;
import cpw.mods.fml.common.Loader;

public enum Mods implements IMod, ITargetMod {

    AE2("appliedenergistics2"),
    AE2_FLUID_CRAFT("ae2fc"),
    AE2_STUFF("ae2stuff"),
    ANGELICA("angelica"),
    ADVENTURE_BACKPACK("adventurebackpack"),
    ASPECT_RECIPE_INDEX("aspectrecipeindex"),
    BACKPACK("Backpack"),
    BAUBLES("Baubles", () -> Loader.isModLoaded("Baubles") || Loader.isModLoaded("Baubles|Expanded"), null),
    BETTER_P2P("betterp2p"),
    BLOCK_RENDERER("blockrenderer6343"),
    BOTANIA("Botania"),
    CORE_MOD("dreamcraft"),
    FIND_IT("findit"),
    FORESTRY("Forestry"),
    GREGTECH("gregtech"),
    HBM_AE_ADDON("hbmaeaddon"),
    HODGEPODGE("hodgepodge"),
    IC2("IC2"),
    NECHAR("nechar"),
    NECH("nech"),
    NOT_ENOUGH_ENERGISTICS("neenergistics"),
    NOT_ENOUGH_ITEMS("NotEnoughItems"),
    OK_BACKPACK("okbackpack"),
    PROGRAMMABLE_HATCHES("programmablehatches"),
    THAUMCRAFT("Thaumcraft"),
    THAUMIC_ENERGISTICS("thaumicenergistics"),
    TINKERS_CONSTRUCT("TConstruct"),
    WAILA("Waila"),
    WIRELESS_CRAFTING_TERMINAL("ae2wct");

    private final String modid;
    private final String resourceDomain;
    private final Supplier<Boolean> supplier;
    private final TargetModBuilder targetBuilder;
    private Boolean loaded;

    Mods(String modid) {
        this(modid, null, null);
    }

    Mods(String modid, Supplier<Boolean> supplier, String coreModClass) {
        this.modid = modid;
        this.resourceDomain = modid.toLowerCase(Locale.ENGLISH);
        this.supplier = supplier;
        this.targetBuilder = new TargetModBuilder().setModId(modid)
            .setCoreModClass(coreModClass);
    }

    @NotNull
    @Override
    public TargetModBuilder getBuilder() {
        return targetBuilder;
    }

    @Override
    public boolean isModLoaded() {
        if (loaded == null) {
            loaded = supplier != null ? supplier.get() : Loader.isModLoaded(modid);
        }
        return loaded;
    }

    @Override
    public String getID() {
        return modid;
    }

    @Override
    public String getResourceLocation() {
        return resourceDomain;
    }

    public static boolean isGt5Loaded() {
        return GREGTECH.isModLoaded() && !Loader.isModLoaded("gregapi");
    }

    public static boolean isGt5UnofficialLoaded() {
        if (!isGt5Loaded()) {
            return false;
        }

        try {
            Class.forName("gregtech.api.recipe.RecipeMap");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static boolean isLegacyGt5Loaded() {
        return isGt5Loaded() && !isGt5UnofficialLoaded();
    }

    public static boolean hasAe2TypeFilter() {
        try {
            Class.forName("appeng.core.features.registries.ItemDisplayRegistry");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static boolean hasCraftingStatusSetting() {
        try {
            Settings.class.getDeclaredField("CRAFTING_STATUS");
            return true;
        } catch (NoSuchFieldException ignored) {
            return false;
        }
    }

    public static boolean hasDoubleButton() {
        try {
            ActionItems.class.getDeclaredField("DOUBLE");
            return true;
        } catch (NoSuchFieldException ignored) {
            return false;
        }
    }

    public static boolean hasBeSubstitutionsButton() {
        try {
            ButtonToolTips.class.getDeclaredField("BeSubstitutionsDescEnabled");
            return true;
        } catch (NoSuchFieldException ignored) {
            return false;
        }
    }
}
