package com.asdflj.ae2thing.common.item;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PhialAmountConverterTest {

    private static final long ESSENTIA_PER_ITEM = 144;

    @Test
    public void reportsExtractionWhenTheCurrentTotalDropsBelowAnItemBoundary() {
        assertEquals(-1, PhialAmountConverter.calculateItemDelta(143, -1, ESSENTIA_PER_ITEM));
    }

    @Test
    public void reportsInjectionWhenTheCurrentTotalReachesAnItemBoundary() {
        assertEquals(1, PhialAmountConverter.calculateItemDelta(144, 1, ESSENTIA_PER_ITEM));
    }

    @Test
    public void ignoresChangesThatStayWithinTheSameItemUnit() {
        assertEquals(0, PhialAmountConverter.calculateItemDelta(142, -1, ESSENTIA_PER_ITEM));
        assertEquals(0, PhialAmountConverter.calculateItemDelta(143, 1, ESSENTIA_PER_ITEM));
    }

    @Test
    public void reportsEveryCrossedItemBoundary() {
        assertEquals(-2, PhialAmountConverter.calculateItemDelta(0, -288, ESSENTIA_PER_ITEM));
        assertEquals(2, PhialAmountConverter.calculateItemDelta(288, 288, ESSENTIA_PER_ITEM));
    }
}
