package com.asdflj.ae2thing.api;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.util.NameConst;
import com.gtnewhorizon.gtnhlib.item.ItemStackNBT;

public class PatternModifierModeObject {

    public enum Mode {

        INSERT(NameConst.PATTERN_MODIFIER_MODE_INSERT),
        SWAP(NameConst.PATTERN_MODIFIER_MODE_SWAP);

        public final String langKey;

        Mode(String langKey) {
            this.langKey = langKey;
        }

        public Mode next() {
            return this == INSERT ? SWAP : INSERT;
        }
    }

    private static final int FALLBACK_WINDOW_TICKS = 1;
    private static final Map<EntityPlayer, Long> LAST_BLOCK_USE = new WeakHashMap<>();

    public static Mode getMode(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return Mode.INSERT;
        byte mode = stack.getTagCompound()
            .getByte(Constants.PATTERN_MODIFIER_MODE);
        return mode >= 0 && mode < Mode.values().length ? Mode.values()[mode] : Mode.INSERT;
    }

    public static void setMode(ItemStack stack, Mode mode) {
        if (stack == null || mode == null) return;
        ItemStackNBT.of(stack)
            .setByte(Constants.PATTERN_MODIFIER_MODE, (byte) mode.ordinal());
    }

    public static Mode nextMode(ItemStack stack) {
        Mode mode = getMode(stack).next();
        setMode(stack, mode);
        return mode;
    }

    public static void markBlockUse(EntityPlayer player) {
        if (player == null || player.worldObj == null) return;
        LAST_BLOCK_USE.put(player, player.worldObj.getTotalWorldTime());
    }

    public static boolean isBlockUseFallback(EntityPlayer player) {
        if (player == null || player.worldObj == null) return false;
        Long tick = LAST_BLOCK_USE.get(player);
        if (tick == null) return false;
        return player.worldObj.getTotalWorldTime() - tick <= FALLBACK_WINDOW_TICKS;
    }
}
