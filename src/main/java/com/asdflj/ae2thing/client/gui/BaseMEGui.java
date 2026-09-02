package com.asdflj.ae2thing.client.gui;

import static com.asdflj.ae2thing.client.render.RenderHelper.drawPinnedSlots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.asdflj.ae2thing.api.AE2ThingAPI;
import com.asdflj.ae2thing.client.gui.widget.IGuiSelection;
import com.asdflj.ae2thing.nei.ButtonConstants;
import com.asdflj.ae2thing.nei.NEI_TH_Config;
import com.asdflj.ae2thing.util.Ae2ReflectClient;

import appeng.api.config.SearchBoxMode;
import appeng.api.config.Settings;
import appeng.api.util.IConfigManager;
import appeng.api.util.IConfigurableObject;
import appeng.client.ActionKey;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.slots.VirtualMEMonitorableSlot;
import appeng.client.gui.slots.VirtualMESlot;
import appeng.core.AEConfig;
import appeng.core.CommonHelper;
import codechicken.nei.LayoutManager;
import codechicken.nei.util.TextHistory;

public abstract class BaseMEGui extends AEBaseGui implements IGuiSelection {

    protected IConfigManager configSrc;
    protected TextHistory history;
    protected final List<VirtualMEMonitorableSlot> meSlots = new ArrayList<>();

    public BaseMEGui(Container container) {
        super(container);
        this.configSrc = ((IConfigurableObject) this.inventorySlots).getConfigManager();
        this.history = Ae2ReflectClient.getHistory(LayoutManager.searchField);
    }

    public List<VirtualMEMonitorableSlot> getMeSlots() {
        return this.meSlots;
    }

    public void registerMESlot(VirtualMEMonitorableSlot slot) {
        this.meSlots.add(slot);
        this.registerVirtualSlots(slot);
    }

    protected boolean isNEISearch() {
        final Enum<?> s = AEConfig.instance.settings.getSetting(Settings.SEARCH_MODE);
        return s == SearchBoxMode.NEI_MANUAL_SEARCH || s == SearchBoxMode.NEI_AUTOSEARCH;
    }

    public boolean hasShiftDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float btn) {
        super.drawScreen(mouseX, mouseY, btn);
        boolean topRowVisible = this.getScrollBar() == null || this.getScrollBar()
            .getCurrentScroll() == 0;
        drawPinnedSlots(this, this.meSlots, this.guiLeft, this.guiTop, topRowVisible);
        this.drawVirtualSlotTooltip(mouseX, mouseY);
    }

    private void drawVirtualSlotTooltip(int mouseX, int mouseY) {
        VirtualMESlot slot = this.getVirtualMESlotUnderMouse();
        if (slot == null) return;
        List<String> lines = new ArrayList<>();
        slot.addTooltip(lines);
        if (!lines.isEmpty()) {
            this.drawHoveringText(lines, mouseX, mouseY, this.fontRendererObj);
        }
    }

    @Override
    public List<String> handleItemTooltip(ItemStack stack, int mouseX, int mouseY, List<String> currentToolTip) {
        super.handleItemTooltip(stack, mouseX, mouseY, currentToolTip);
        VirtualMESlot slot = this.getVirtualMESlotUnderMouse();
        if (slot != null) {
            slot.addTooltip(currentToolTip);
        }
        return currentToolTip;
    }

    @Override
    public void drawHistorySelection(final int x, final int y, String text, int width,
        final List<String> searchHistory) {
        if (!NEI_TH_Config.getConfigValue(ButtonConstants.HISTORY)) return;
        final int maxRows = AE2ThingAPI.maxSelectionRows;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        String[] var4 = null;
        final List<String> history = new ArrayList<>(searchHistory);
        Collections.reverse(history);

        if (history.size() > maxRows) {
            for (int i = 1; i < history.size(); i++) {
                if (text.equals(history.get(i))) {
                    int max = Math.min(history.size(), i + maxRows - 1);
                    int min = Math.max(0, max - maxRows);
                    var4 = history.subList(min, max)
                        .toArray(new String[0]);
                    break;
                }
            }
        }
        if (var4 == null) {
            var4 = history.subList(0, Math.min(history.size(), 5))
                .toArray(new String[0]);
        }
        if (var4.length > 0) {
            int var5 = width;
            int var6;
            int var7;

            for (var6 = 0; var6 < var4.length; ++var6) {
                var7 = this.fontRendererObj.getStringWidth(var4[var6]) + 8;

                if (var7 > var5) {
                    var5 = var7;
                }
            }

            var6 = x + 3;
            var7 = y + 15;
            int var9 = 8;

            if (var4.length > 1) {
                var9 += 2 + (var4.length - 1) * 10;
            }

            if (this.guiTop + var7 + var9 + 6 > this.height) {
                var7 = this.height - var9 - this.guiTop - 6;
            }

            this.zLevel = 300.0F;
            itemRender.zLevel = 300.0F;
            final int var10 = -267386864;
            this.drawGradientRect(var6 - 3, var7 - 4, var6 + var5 + 3, var7 - 3, var10, var10);
            this.drawGradientRect(var6 - 3, var7 + var9 + 3, var6 + var5 + 3, var7 + var9 + 4, var10, var10);
            this.drawGradientRect(var6 - 3, var7 - 3, var6 + var5 + 3, var7 + var9 + 3, var10, var10);
            this.drawGradientRect(var6 - 4, var7 - 3, var6 - 3, var7 + var9 + 3, var10, var10);
            this.drawGradientRect(var6 + var5 + 3, var7 - 3, var6 + var5 + 4, var7 + var9 + 3, var10, var10);
            final int var11 = 1347420415;
            final int var12 = (var11 & 16711422) >> 1 | var11 & -16777216;
            this.drawGradientRect(var6 - 3, var7 - 3 + 1, var6 - 3 + 1, var7 + var9 + 3 - 1, var11, var12);
            this.drawGradientRect(var6 + var5 + 2, var7 - 3 + 1, var6 + var5 + 3, var7 + var9 + 3 - 1, var11, var12);
            this.drawGradientRect(var6 - 3, var7 - 3, var6 + var5 + 3, var7 - 3 + 1, var11, var11);
            this.drawGradientRect(var6 - 3, var7 + var9 + 2, var6 + var5 + 3, var7 + var9 + 3, var12, var12);

            for (int var13 = 0; var13 < var4.length; ++var13) {
                String var14 = var4[var13];
                if (var14.equals(text)) {
                    var14 = "> " + var14;
                    var14 = '\u00a7' + Integer.toHexString(15) + var14;
                } else {
                    var14 = "\u00a77" + var14;
                }

                this.fontRendererObj.drawStringWithShadow(var14, var6, var7, -1);

                if (var13 == 0) {
                    var7 += 2;
                }

                var7 += 10;
            }

            this.zLevel = 0.0F;
            itemRender.zLevel = 0.0F;
        }
        GL11.glPopAttrib();
    }

    public abstract int getOffsetY();

    public void initDone() {

    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (this instanceof IGuiMonitorTerminal gmt
            && CommonHelper.proxy.isActionKey(ActionKey.TOGGLE_FOCUS, keyCode)) {
            gmt.getSearchField()
                .setFocused(
                    !gmt.getSearchField()
                        .isFocused());
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }
}
