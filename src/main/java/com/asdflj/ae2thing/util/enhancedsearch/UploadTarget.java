package com.asdflj.ae2thing.util.enhancedsearch;

import java.util.List;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class UploadTarget {

    public enum Outcome {
        TARGET,
        NAME_TARGET,
        SKIP
    }

    private UploadTarget() {}

    public static Outcome outcome(ItemStack rep, String name, String suffix, List<String> poolIds, String nameHint) {
        return switch (InterfacePoolResolver.classify(rep, poolIds)) {
            case MATCH -> Outcome.TARGET;
            case NO_MATCH -> Outcome.SKIP;
            case UNKNOWN -> EnhancedSearch.matchesNameHint(nameHint, name, suffix) ? Outcome.NAME_TARGET : Outcome.SKIP;
        };
    }
}
