package com.asdflj.ae2thing.common.item;

final class PhialAmountConverter {

    private PhialAmountConverter() {}

    /**
     * Calculates the facade delta from the total after a change and that signed change. Both totals are converted
     * independently so a sub-unit change crossing an item boundary is not rounded away.
     */
    static long calculateItemDelta(long currentAmount, long signedDelta, long amountPerUnit) {
        if (amountPerUnit <= 0) throw new IllegalArgumentException("amountPerUnit must be positive");

        currentAmount = Math.max(0, currentAmount);
        final long previousAmount;
        try {
            previousAmount = Math.max(0, Math.subtractExact(currentAmount, signedDelta));
        } catch (ArithmeticException overflow) {
            // A negative delta can only overflow toward a previous amount larger than Long.MAX_VALUE. Saturating keeps
            // listener notification safe for malformed third-party deltas without changing normal storage semantics.
            return currentAmount / amountPerUnit - (signedDelta < 0 ? Long.MAX_VALUE / amountPerUnit : 0);
        }
        return currentAmount / amountPerUnit - previousAmount / amountPerUnit;
    }
}
