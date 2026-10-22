package com.asdflj.ae2thing.nei;

import appeng.util.Platform;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import com.asdflj.ae2thing.integration.Mods;
import com.github.vfyjxf.nee.processor.IRecipeProcessor;
import com.github.vfyjxf.nee.processor.RecipeProcessor;
import com.github.vfyjxf.nee.utils.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NEEUtils {

    public static final String NULL_IDENTIFIER = "null";

    private static final Set<String> SKIPPED_PROCESSORS = new HashSet<>();

    static {
        SKIPPED_PROCESSORS.add("Gregtech5");
        SKIPPED_PROCESSORS.add("Gregtech6");
    }

    private NEEUtils() {}

    public static String realIdentifierOf(IRecipeHandler recipe) {
        if (recipe instanceof TemplateRecipeHandler template) {
            final String realIdentifier = template.getOverlayIdentifier();
            return realIdentifier == null ? NULL_IDENTIFIER : realIdentifier;
        }
        return NULL_IDENTIFIER;
    }

    public static Set<String> allIdentifiers() {
        final Set<String> identifiers = new HashSet<>();
        try {
            for (IRecipeProcessor processor : RecipeProcessor.recipeProcessors) {
                identifiers.addAll(processor.getAllOverlayIdentifier());
            }
        } catch (Throwable ignored) {}
        return identifiers;
    }

    public static IRecipeProcessor find(IRecipeHandler recipe, int recipeIndex, String identifier) {
        try {
            for (IRecipeProcessor processor : RecipeProcessor.recipeProcessors) {
                if (SKIPPED_PROCESSORS.contains(processor.getRecipeProcessorId())) continue;
                if (!processor.getAllOverlayIdentifier().contains(identifier)) continue;
                if (processor.getRecipeInput(recipe, recipeIndex, identifier).isEmpty()) continue;
                if (processor.getRecipeOutput(recipe, recipeIndex, identifier).isEmpty()) continue;
                return processor;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static List<PositionedStack> getInputs(IRecipeHandler recipe, int recipeIndex, String identifier) {
        final IRecipeProcessor processor = find(recipe, recipeIndex, identifier);
        if (processor == null) return Collections.emptyList();

        try {
            return new ArrayList<>(processor.getRecipeInput(recipe, recipeIndex, identifier));
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
    }

    public static List<PositionedStack> getOutputs(IRecipeHandler recipe, int recipeIndex, String identifier) {
        final IRecipeProcessor processor = find(recipe, recipeIndex, identifier);
        if (processor == null) return Collections.emptyList();

        try {
            return new ArrayList<>(processor.getRecipeOutput(recipe, recipeIndex, identifier));
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
    }

    public static boolean mergeStacks(IRecipeHandler recipe, int recipeIndex, String identifier) {
        final IRecipeProcessor processor = find(recipe, recipeIndex, identifier);
        if (processor == null) return true;
        try {
            return processor.mergeStacks(recipe, recipeIndex, identifier);
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static ItemStack preferModItem(ItemStack[] items) {
        if (items == null || items.length == 0) return null;
        try {
            return ItemUtils.getPreferModItem(items);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isPreferItem(ItemStack stack, String identifier) {
        try {
            return ItemUtils.isPreferItems(stack, "", identifier);
        } catch (Throwable ignored) {
            try {
                return ItemUtils.isPreferItems(stack);
            } catch (Throwable ignored2) {
                return false;
            }
        }
    }

    public static boolean isBlacklisted(ItemStack stack, String identifier) {
        try {
            return ItemUtils.isInBlackList(stack, "", identifier);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static final class NEIStackSelector {

        private static final String GT_TOOL_CLASS = "gregtech.api.items.MetaGeneratedTool";
        private static Class<?> gtToolClass;
        private static boolean gtToolLookedUp;

        private NEIStackSelector() {}

        public static ItemStack select(PositionedStack ps, String identifier, boolean output) {
            if (ps == null || ps.items == null || ps.items.length == 0) return null;

            final boolean shift = NEIClientUtils.shiftKey();
            ItemStack chosen;
            if (shift && ps.item != null) {
                chosen = ps.item.copy();
            } else {
                final List<ItemStack> filtered = ps.getFilteredPermutations();
                chosen = (filtered.isEmpty() ? ps.items[0] : filtered.getFirst()).copy();
            }
            if (chosen.getItem() == null) return null;

            if (!output) {
                final ItemStack preferedMod = Mods.NOT_ENOUGH_ENERGISTICS.isModLoaded() ? NEEUtils.preferModItem(ps.items) : null;
                if (preferedMod != null) chosen = preferedMod.copy();

                for (ItemStack candidate : ps.items) {
                    if (candidate == null || candidate.getItem() == null) continue;
                    if (Platform.isRecipePrioritized(candidate) || Mods.NOT_ENOUGH_ENERGISTICS.isModLoaded() && NEEUtils.isPreferItem(candidate, identifier)) {
                        chosen = candidate.copy();
                        break;
                    }
                }

                if (Mods.NOT_ENOUGH_ENERGISTICS.isModLoaded() && NEEUtils.isBlacklisted(chosen, identifier)) return null;
            }

            if (!output) transformGTTool(chosen);

            if (chosen.getItemDamage() == OreDictionary.WILDCARD_VALUE) chosen.setItemDamage(0);
            if (chosen.stackSize < 1) chosen.stackSize = 1;
            return chosen;
        }

        public static void transformGTTool(ItemStack stack) {
            if (stack == null || stack.hasTagCompound()) return;
            if (!Mods.isGt5Loaded()) return;
            if (!gtToolLookedUp) {
                gtToolLookedUp = true;
                try {
                    gtToolClass = Class.forName(GT_TOOL_CLASS);
                } catch (ClassNotFoundException ignored) {}
            }
            if (gtToolClass == null || stack.getItem() == null || !gtToolClass.isAssignableFrom(stack.getItem().getClass())) return;

            final NBTTagCompound nbt = new NBTTagCompound();
            final NBTTagCompound toolStats = new NBTTagCompound();
            toolStats.setString("PrimaryMaterial", "Neutronium");
            toolStats.setString("SecondaryMaterial", "Neutronium");
            toolStats.setInteger("MaxDamage", 65536000);
            nbt.setTag("GT.ToolStats", toolStats);
            stack.setTagCompound(nbt);
        }
    }
}
