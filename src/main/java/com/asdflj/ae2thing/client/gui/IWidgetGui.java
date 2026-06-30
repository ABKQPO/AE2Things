package com.asdflj.ae2thing.client.gui;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.inventory.Slot;

import com.asdflj.ae2thing.client.gui.widget.IAEBasePanel;

import appeng.client.gui.slots.VirtualMEMonitorableSlot;

public interface IWidgetGui {

    BaseMEGui getGui();

    boolean hideItemPanelSlot(int x, int y, int w, int h);

    List<GuiButton> getButtonList();

    IAEBasePanel getActivePanel();

    List<VirtualMEMonitorableSlot> getMeSlots();

    void registerMESlot(VirtualMEMonitorableSlot slot);

    RenderItem getRenderItem();

    Slot getSlot(final int mouseX, final int mouseY);
}
