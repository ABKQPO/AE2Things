package com.asdflj.ae2thing.inventory.gui;

import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Self-contained encoder/decoder for the inventory source carried in a GUI-open x coordinate.
 * AE2FluidCraft-Rework's {@code Util.GuiHelper} dropped the player/baubles distinction (its enum is
 * now {@code TILE}/{@code ITEM}), so AE2Things owns this small bit-packing to keep routing terminal
 * items between the main inventory and Baubles slots.
 */
public enum GuiBridgeInvType {

    PLAYER_INV,
    PLAYER_BAUBLES;

    private static final int FLAG = 1 << 30;
    private static final int LIMIT = 1 << 28;

    public static int encode(int slot, GuiBridgeInvType type) {
        if (Math.abs(slot) > LIMIT) {
            throw new IllegalArgumentException("slot out of range");
        }
        return FLAG | (type.ordinal() << 29) | slot;
    }

    public static ImmutablePair<GuiBridgeInvType, Integer> decode(int value) {
        if (Math.abs(value) > LIMIT) {
            return new ImmutablePair<>(values()[value >> 29 & 1], value - (3 << 29 & value));
        }
        return new ImmutablePair<>(PLAYER_INV, value);
    }
}
