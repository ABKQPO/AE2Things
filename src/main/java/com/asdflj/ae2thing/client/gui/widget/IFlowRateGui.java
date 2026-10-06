package com.asdflj.ae2thing.client.gui.widget;

import java.util.Map;

import appeng.api.storage.data.IAEStack;
import appeng.me.cache.ItemFlowGridCache.FlowRate;

/**
 * Implemented by terminal GUIs that can display item flow rates, so {@code SPacketFlowRates} has an interface to
 * deliver
 * to. AE2's own packet checks for {@code GuiMEMonitorable} directly, which terminals that do not extend it can never
 * match.
 */
public interface IFlowRateGui {

    void updateFlowRates(Map<IAEStack<?>, FlowRate> rates);
}
