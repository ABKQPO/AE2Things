package com.asdflj.ae2thing.nei;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.nei.object.OrderStack;

import appeng.util.Platform;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.StackInfo;

public final class NEIStackSelector {

    private static final Set<String> CRAFTING_TABLE_IDENTIFIERS = Set.of("crafting", "crafting2x2");

    private static final String GT_TOOL_CLASS = "gregtech.api.items.MetaGeneratedTool";

    private static Class<?> gtToolClass;
    private static boolean gtToolLookedUp;

    private NEIStackSelector() {}

    public static boolean isCraftingTable(String identifier) {
        return identifier != null && CRAFTING_TABLE_IDENTIFIERS.contains(identifier);
    }

    public static OrderStack<?> pack(PositionedStack positionedStack, int index, String identifier, boolean output) {
        if (positionedStack == null) return null;
        final ItemStack selected = select(positionedStack, identifier, output);
        if (selected == null) return null;
        final FluidStack fluid = StackInfo.getFluid(selected);
        if (fluid != null) return new OrderStack<>(fluid, index);
        final ItemStack[] permutations = positionedStack.items;
        if (permutations == null || permutations.length < 2) return new OrderStack<>(selected, index);
        return new OrderStack<>(selected, index, permutations);
    }

    public static ItemStack select(PositionedStack positionedStack, String identifier, boolean output) {
        if (positionedStack == null) return null;
        final ItemStack base = output ? displayedStack(positionedStack) : baseStack(positionedStack);
        if (base == null) return null;

        final int size = base.stackSize;
        final ItemStack[] permutations = positionedStack.items;
        final boolean craftingTable = isCraftingTable(identifier);
        ItemStack chosen = base;

        if (!output) {
            final ItemStack preferMod = preferModItem(permutations);
            if (preferMod != null) chosen = copyWithSize(preferMod, size);

            if (permutations != null) {
                for (ItemStack candidate : permutations) {
                    if (candidate == null || candidate.getItem() == null) continue;
                    if (Platform.isRecipePrioritized(candidate) || isPreferItem(candidate, identifier)) {
                        chosen = copyWithSize(candidate, size);
                        break;
                    }
                }
            }

            if (!craftingTable && isBlacklisted(chosen, identifier)) return null;

            if (craftingTable) transformGTTool(chosen);
        }

        if (chosen.getItemDamage() == OreDictionary.WILDCARD_VALUE) chosen.setItemDamage(0);
        return chosen;
    }

    private static ItemStack displayedStack(PositionedStack positionedStack) {
        if (isValid(positionedStack.item)) return positionedStack.item.copy();
        return baseStack(positionedStack);
    }

    private static ItemStack baseStack(PositionedStack positionedStack) {
        final ItemStack[] permutations = positionedStack.items;
        if (permutations == null || permutations.length == 0) {
            return isValid(positionedStack.item) ? positionedStack.item.copy() : null;
        }
        if (NEIClientUtils.shiftKey() && isValid(positionedStack.item)) {
            return positionedStack.item.copy();
        }
        final List<ItemStack> filtered = positionedStack.getFilteredPermutations();
        for (ItemStack candidate : filtered) {
            if (isValid(candidate)) return candidate.copy();
        }
        for (ItemStack candidate : permutations) {
            if (isValid(candidate)) return candidate.copy();
        }
        return null;
    }

    private static boolean isValid(ItemStack stack) {
        return stack != null && stack.getItem() != null;
    }

    private static ItemStack copyWithSize(ItemStack stack, int size) {
        final ItemStack copy = stack.copy();
        copy.stackSize = size;
        return copy;
    }

    private static ItemStack preferModItem(ItemStack[] permutations) {
        if (permutations == null || permutations.length == 0) return null;
        if (!Mods.NOT_ENOUGH_ENERGISTICS.isModLoaded()) return null;
        return NEEUtils.preferModItem(permutations);
    }

    private static boolean isPreferItem(ItemStack stack, String identifier) {
        if (!Mods.NOT_ENOUGH_ENERGISTICS.isModLoaded()) return false;
        return NEEUtils.isPreferItem(stack, identifier);
    }

    private static boolean isBlacklisted(ItemStack stack, String identifier) {
        if (!Mods.NOT_ENOUGH_ENERGISTICS.isModLoaded()) return false;
        return NEEUtils.isBlacklisted(stack, identifier);
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
        if (gtToolClass == null || !isValid(stack)
            || !gtToolClass.isAssignableFrom(
                Objects.requireNonNull(stack.getItem())
                    .getClass()))
            return;

        final NBTTagCompound nbt = new NBTTagCompound();
        final NBTTagCompound toolStats = new NBTTagCompound();
        toolStats.setString("PrimaryMaterial", "Neutronium");
        toolStats.setString("SecondaryMaterial", "Neutronium");
        toolStats.setInteger("MaxDamage", 65536000);
        nbt.setTag("GT.ToolStats", toolStats);
        stack.setTagCompound(nbt);
    }
}
