package com.asdflj.ae2thing.api.adapter.terminal;

import org.jetbrains.annotations.Nullable;

import appeng.api.storage.data.IAEStack;

public interface IGuiCraftAmount {

    default int getAmount() {
        return (int) Math.min(getAmountLong(), Integer.MAX_VALUE);
    }

    long getAmountLong();

    void setAmount(long amount);

    @Nullable
    IAEStack<?> getTarget();
}
