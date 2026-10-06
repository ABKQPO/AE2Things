package com.asdflj.ae2thing.client.gui;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.client.gui.container.ContainerMonitor;
import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.client.gui.container.slot.SlotPatternFake;
import com.asdflj.ae2thing.client.gui.widget.IAEBasePanel;
import com.asdflj.ae2thing.client.gui.widget.IDraggable;
import com.asdflj.ae2thing.client.gui.widget.IFlowRateGui;
import com.asdflj.ae2thing.client.gui.widget.IGuiMonitor;
import com.asdflj.ae2thing.client.gui.widget.IGuiSelection;
import com.asdflj.ae2thing.client.gui.widget.ITypeFilterGui;
import com.asdflj.ae2thing.client.gui.widget.ItemPanel;
import com.asdflj.ae2thing.client.gui.widget.PanelDragButton;
import com.asdflj.ae2thing.client.gui.widget.PatternPanel;
import com.asdflj.ae2thing.client.gui.widget.THGuiTextField;
import com.asdflj.ae2thing.client.me.AdvItemRepo;
import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.network.CPacketSwitchGuis;
import com.asdflj.ae2thing.network.CPacketTerminalBtns;

import appeng.api.config.Settings;
import appeng.api.parts.IPatternTerminal;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.util.IConfigManager;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.slots.VirtualMEMonitorableSlot;
import appeng.client.gui.slots.VirtualMESlot;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.gui.widgets.IDropToFillTextField;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.container.slot.SlotPatternTerm;
import appeng.container.slot.SlotRestrictedInput;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.me.cache.ItemFlowGridCache.FlowRate;
import appeng.util.IConfigManagerHost;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;

public class GuiWirelessDualInterfaceTerminal extends GuiBaseInterfaceWireless implements IWidgetGui, IGuiDrawSlot,
    IGuiMonitorTerminal, IConfigManagerHost, IGuiSelection, IDropToFillTextField, ITypeFilterGui, IFlowRateGui {

    public ContainerWirelessDualInterfaceTerminal container;
    private GuiTabButton craftingStatusBtn;
    private final int baseXSize;
    private static final int fullXSize = 1000;
    private final List<IAEBasePanel> panels = new ArrayList<>();
    private IAEBasePanel activePanel = null;
    private IAEBasePanel pointerPanel = null;
    private Point mouse;
    private boolean dragging = false;
    private int lastMouseX;
    private int lastMouseY;
    private PanelDragButton patternDragButton;
    private PanelDragButton itemDragButton;
    private boolean panelPositionsDirty;
    private final ItemPanel itemPanel;
    private IAEItemStack blankPatternView = IPatternTerminal.createBlankPattern()
        .setStackSize(0);

    public GuiWirelessDualInterfaceTerminal(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(inventoryPlayer, te);
        container = (ContainerWirelessDualInterfaceTerminal) this.inventorySlots;
        this.itemPanel = new ItemPanel(this, container, this.configSrc);
        this.panels.add(new PatternPanel(this, container));
        this.panels.add(this.itemPanel);
        ((ContainerMonitor) this.inventorySlots).setGui(this);
        this.baseXSize = this.xSize;
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        for (IAEBasePanel panel : this.panels) {
            if (panel.isActive()) {
                this.resetPanelColor();
                panel.drawFG(offsetX, offsetY, mouseX, mouseY);
                this.resetPanelColor();
            }
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        for (IAEBasePanel panel : this.panels) {
            if (panel.isActive()) {
                this.resetPanelColor();
                panel.drawBG(offsetX, offsetY, mouseX, mouseY);
                this.resetPanelColor();
            }
        }
    }

    @Override
    public boolean isOverTextField(int mousex, int mousey) {
        if (super.isOverTextField(mousex, mousey)) {
            return true;
        }
        return this.itemPanel.isOverTextField(mousex, mousey);
    }

    @Override
    public void setTextFieldValue(String displayName, int mousex, int mousey, ItemStack stack) {
        super.setTextFieldValue(displayName, mousex, mousey, stack);
        this.itemPanel.setTextFieldValue(displayName, mousex, mousey, stack);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float btn) {
        this.xSize = baseXSize;
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        if (dragging) {
            this.moveActivePanel();
        }
        for (IAEBasePanel panel : this.panels) {
            if (panel.isActive()) {
                panel.drawScreen(mouseX, mouseY, btn);
            }
        }
        this.updatePanelSlots();
        this.updateDragButtons();
        if (this.itemPanel.getRepo()
            .hasCache()) {
            try {
                this.itemPanel.getRepo()
                    .getLock()
                    .lock();
                super.drawScreen(mouseX, mouseY, btn);
            } finally {
                this.itemPanel.getRepo()
                    .getLock()
                    .unlock();
            }
        } else {
            super.drawScreen(mouseX, mouseY, btn);
        }
        this.xSize = fullXSize;
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        if (btn == 0) {
            for (int index = this.panels.size() - 1; index >= 0; index--) {
                IAEBasePanel panel = this.panels.get(index);
                if (panel.isActive() && panel.draggable() && this.isInside(panel.getDragHandle(), xCoord, yCoord)) {
                    this.beginDrag(panel, xCoord, yCoord);
                    return;
                }
            }
        }
        this.pointerPanel = this.findTopPanel(xCoord, yCoord);
        if (this.pointerPanel != null) {
            this.pointerPanel.mouseClicked(xCoord, yCoord, btn);
        }
        super.mouseClicked(xCoord, yCoord, btn);
    }

    @Override
    protected void mouseClickMove(final int x, final int y, final int c, final long d) {
        this.lastMouseX = x;
        this.lastMouseY = y;
        if (this.dragging) {
            this.moveActivePanel();
            return;
        }
        if (this.pointerPanel != null) {
            this.pointerPanel.mouseClickMove(x, y, c, d);
        } else {
            for (IAEBasePanel panel : this.panels) {
                if (panel.isActive()) {
                    panel.mouseClickMove(x, y, c, d);
                }
            }
        }
        if (this.pointerPanel != null) return;
        super.mouseClickMove(x, y, c, d);
    }

    @Override
    public void handleMouseInput() {
        if (Mouse.getEventButton() != -1 && !Mouse.getEventButtonState()) {
            if (Mouse.getEventButton() == 0) {
                if (this.dragging) {
                    this.sendPanelPositions();
                }
                this.activePanel = null;
                this.mouse = null;
                this.dragging = false;
            }
            this.pointerPanel = null;
        }
        super.handleMouseInput();
    }

    @Override
    protected void handleMouseClick(Slot slot, int slotIdx, int ctrlDown, int mouseButton) {
        if (this.pointerPanel != null) {
            if (this.pointerPanel.handleMouseClick(slot, slotIdx, ctrlDown, mouseButton)) return;
            if (this.pointerPanel instanceof PatternPanel && this.isPatternSlot(slot)) {
                super.handleMouseClick(slot, slotIdx, ctrlDown, mouseButton);
            }
            return;
        }
        if (slot instanceof AppEngSlot appEngSlot && appEngSlot.isPlayerSide()) {
            super.handleMouseClick(slot, slotIdx, ctrlDown, mouseButton);
            return;
        }
        for (int index = this.panels.size() - 1; index >= 0; index--) {
            IAEBasePanel panel = this.panels.get(index);
            if (!panel.isActive()) continue;
            if (panel.handleMouseClick(slot, slotIdx, ctrlDown, mouseButton)) return;
        }
        if (slotIdx < 0) return;
        super.handleMouseClick(slot, slotIdx, ctrlDown, mouseButton);
    }

    @Override
    protected boolean handleVirtualSlotClick(VirtualMESlot slot, int mouseButton) {
        if (this.pointerPanel != null) {
            this.pointerPanel.handleVirtualSlotClick(slot, mouseButton);
            return true;
        }
        for (int index = this.panels.size() - 1; index >= 0; index--) {
            IAEBasePanel panel = this.panels.get(index);
            if (!panel.isActive()) continue;
            if (panel.handleVirtualSlotClick(slot, mouseButton)) return true;
        }
        return super.handleVirtualSlotClick(slot, mouseButton);
    }

    @Override
    protected boolean mouseWheelEvent(int mouseX, int mouseY, int wheel) {
        IAEBasePanel topPanel = this.findTopPanel(mouseX, mouseY);
        if (topPanel != null) {
            topPanel.mouseWheelEvent(mouseX, mouseY, wheel);
            return true;
        }
        for (int index = this.panels.size() - 1; index >= 0; index--) {
            IAEBasePanel panel = this.panels.get(index);
            if (!panel.isActive()) continue;
            if (panel.mouseWheelEvent(mouseX, mouseY, wheel)) return true;
        }
        return super.mouseWheelEvent(mouseX, mouseY, wheel);
    }

    @Override
    protected void keyTyped(char character, int key) {
        this.xSize = baseXSize;
        for (int index = this.panels.size() - 1; index >= 0; index--) {
            IAEBasePanel panel = this.panels.get(index);
            if (!panel.isActive()) continue;
            if (!this.checkHotbarKeys(key) && panel.keyTyped(character, key)) return;
        }
        super.keyTyped(character, key);
    }

    @Override
    public void func_146977_a(final Slot s) {
        if (s == this.container.getContainer()
            .getPatternInputSlot() && !s.getHasStack()) {
            this.blankPatternView.drawInGui(this.mc, s.xDisplayPosition, s.yDisplayPosition);
            this.blankPatternView
                .drawOverlayInGui(this.mc, s.xDisplayPosition, s.yDisplayPosition, true, true, true, true);
            return;
        }
        if (drawSlot(s, () -> super.func_146977_a(s))) super.func_146977_a(s);
    }

    @Override
    public List<String> handleItemTooltip(ItemStack stack, int mouseX, int mouseY, List<String> lines) {
        super.handleItemTooltip(stack, mouseX, mouseY, lines);
        Slot input = this.container.getContainer()
            .getPatternInputSlot();
        if (this.getSlot(mouseX, mouseY) == input && !input.getHasStack()) {
            lines.add(GuiText.BlankPatternInNetwork.getLocal());
            lines.add(String.format(ButtonToolTips.ItemsStored.getLocal(), this.blankPatternView.getStackSize()));
        }
        return lines;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.xSize = baseXSize;
        super.initGui();
        for (IAEBasePanel panel : this.panels) {
            panel.initGui();
        }
        this.restorePanelPositions();
        this.patternDragButton = new PanelDragButton(0, 0);
        this.itemDragButton = new PanelDragButton(0, 0);
        this.buttonList.add(this.patternDragButton);
        this.buttonList.add(this.itemDragButton);
        this.updateDragButtons();
        this.buttonList.add(
            this.craftingStatusBtn = new GuiTabButton(
                this.guiLeft + 184,
                this.guiTop - 4,
                2 + 11 * 16,
                GuiText.CraftingStatus.getLocal(),
                itemRender));
        this.craftingStatusBtn.setHideEdge(13); // GuiTabButton implementation //
    }

    @Override
    public BaseMEGui getGui() {
        return this;
    }

    @Override
    public void onGuiClosed() {
        this.sendPanelPositions();
        super.onGuiClosed();
        for (IAEBasePanel panel : this.panels) {
            panel.onGuiClosed();
        }
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean hideItemPanelSlot(int x, int y, int w, int h) {
        for (IAEBasePanel panel : this.panels) {
            if (!panel.isActive()) continue;
            if (panel.hideItemPanelSlot(x, y, w, h)) return true;
        }
        return false;
    }

    @Override
    public List<GuiButton> getButtonList() {
        return this.buttonList;
    }

    @Override
    public IAEBasePanel getActivePanel() {
        return this.activePanel;
    }

    @Override
    public List<VirtualMEMonitorableSlot> getMeSlots() {
        return super.getMeSlots();
    }

    @Override
    public RenderItem getRenderItem() {
        return itemRender;
    }

    @Override
    public Slot getSlot(int mouseX, int mouseY) {
        return super.getSlot(mouseX, mouseY);
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        if (this.pointerPanel != null) {
            this.pointerPanel.actionPerformed(btn);
            return;
        }
        if (this.craftingStatusBtn == btn) {
            AE2Thing.proxy.netHandler.sendToServer(new CPacketSwitchGuis(GuiType.CRAFTING_STATUS_ITEM));
            return;
        }
        for (int index = this.panels.size() - 1; index >= 0; index--) {
            IAEBasePanel panel = this.panels.get(index);
            if (panel.actionPerformed(btn)) return;
        }
        super.actionPerformed(btn);
    }

    private void beginDrag(IAEBasePanel panel, int mouseX, int mouseY) {
        IDraggable.Rectangle rectangle = panel.getRectangle();
        this.activePanel = panel;
        this.mouse = new Point(mouseX - rectangle.x(), mouseY - rectangle.y());
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.dragging = true;
        this.bringToFront(panel);
        this.moveActivePanel();
    }

    private void moveActivePanel() {
        if (!this.dragging || this.activePanel == null || this.mouse == null) return;
        IDraggable.Rectangle rectangle = this.activePanel.getRectangle();
        int x = this.lastMouseX - this.mouse.x;
        int y = this.lastMouseY - this.mouse.y;
        x = Math.max(2, Math.min(x, this.width - rectangle.width() - 2));
        y = Math.max(2, Math.min(y, this.height - rectangle.height() - 2));
        this.panelPositionsDirty |= x != rectangle.x() || y != rectangle.y();
        this.activePanel.move(x, y);
    }

    private void restorePanelPositions() {
        PatternPanel patternPanel = this.findPanel(PatternPanel.class);
        ItemPanel itemPanel = this.findPanel(ItemPanel.class);
        if (patternPanel == null || itemPanel == null) return;
        NBTTagCompound positions = this.container.getPanelPositions();
        IDraggable.Rectangle patternRectangle = patternPanel.getRectangle();
        IDraggable.Rectangle itemRectangle = itemPanel.getRectangle();
        patternPanel.setRectangle(
            this.clampPanelX(
                this.getPanelPosition(positions, "patternX", patternRectangle.x()),
                patternRectangle.width()),
            this.clampPanelY(
                this.getPanelPosition(positions, "patternY", patternRectangle.y()),
                patternRectangle.height()));
        itemPanel.setRectangle(
            this.clampPanelX(this.getPanelPosition(positions, "itemX", itemRectangle.x()), itemRectangle.width()),
            this.clampPanelY(this.getPanelPosition(positions, "itemY", itemRectangle.y()), itemRectangle.height()));
    }

    private int getPanelPosition(NBTTagCompound positions, String key, int defaultValue) {
        return positions.hasKey(key) ? positions.getInteger(key) : defaultValue;
    }

    private void sendPanelPositions() {
        if (!this.panelPositionsDirty) return;
        PatternPanel patternPanel = this.findPanel(PatternPanel.class);
        ItemPanel itemPanel = this.findPanel(ItemPanel.class);
        if (patternPanel == null || itemPanel == null) return;
        IDraggable.Rectangle patternRectangle = patternPanel.getRectangle();
        IDraggable.Rectangle itemRectangle = itemPanel.getRectangle();
        NBTTagCompound positions = new NBTTagCompound();
        positions.setInteger("patternX", patternRectangle.x());
        positions.setInteger("patternY", patternRectangle.y());
        positions.setInteger("itemX", itemRectangle.x());
        positions.setInteger("itemY", itemRectangle.y());
        AE2Thing.proxy.netHandler
            .sendToServer(new CPacketTerminalBtns("DualInterfaceTerminal.PanelPositions", 0, positions));
        this.panelPositionsDirty = false;
    }

    private int clampPanelX(int x, int width) {
        return Math.max(2, Math.min(x, this.width - width - 2));
    }

    private int clampPanelY(int y, int height) {
        return Math.max(2, Math.min(y, this.height - height - 2));
    }

    private void resetPanelColor() {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void updatePanelSlots() {
        for (IAEBasePanel panel : this.panels) {
            if (panel instanceof PatternPanel patternPanel) {
                patternPanel.updateSlotPositions();
            }
        }
    }

    private void updateDragButtons() {
        this.updateDragButton(this.patternDragButton, this.findPanel(PatternPanel.class));
        this.updateDragButton(this.itemDragButton, this.findPanel(ItemPanel.class));
    }

    private void updateDragButton(PanelDragButton button, IAEBasePanel panel) {
        if (button == null || panel == null) return;
        IDraggable.Rectangle handle = panel.getDragHandle();
        button.xPosition = handle.x();
        button.yPosition = handle.y();
        button.visible = panel.isActive();
    }

    private <T extends IAEBasePanel> T findPanel(Class<T> type) {
        for (IAEBasePanel panel : this.panels) {
            if (type.isInstance(panel)) return type.cast(panel);
        }
        return null;
    }

    private void bringToFront(IAEBasePanel panel) {
        if (this.panels.get(this.panels.size() - 1) != panel) {
            this.panels.remove(panel);
            this.panels.add(panel);
        }
        PanelDragButton dragButton = panel instanceof PatternPanel ? this.patternDragButton : this.itemDragButton;
        if (dragButton != null && this.buttonList.remove(dragButton)) {
            this.buttonList.add(dragButton);
        }
    }

    private boolean isInside(IDraggable.Rectangle rectangle, int x, int y) {
        return x >= rectangle.x() && x < rectangle.x() + rectangle.width()
            && y >= rectangle.y()
            && y < rectangle.y() + rectangle.height();
    }

    private IAEBasePanel findTopPanel(int x, int y) {
        for (int index = this.panels.size() - 1; index >= 0; index--) {
            IAEBasePanel panel = this.panels.get(index);
            if (panel.isActive() && this.isInside(panel.getRectangle(), x, y)) return panel;
        }
        return null;
    }

    private boolean isPatternSlot(Slot slot) {
        return slot instanceof SlotPatternFake || slot instanceof SlotFakeCraftingMatrix
            || slot instanceof SlotPatternTerm
            || slot instanceof SlotRestrictedInput;
    }

    @Override
    protected void repositionSlots() {
        for (final Object obj : this.inventorySlots.inventorySlots) {
            if(obj instanceof SlotPatternFake s){
                s.yDisplayPosition = this.ySize + s.getY() - this.viewHeight - 78 - 4;
            } else if (obj instanceof SlotRestrictedInput s) {
                s.yDisplayPosition = this.ySize + s.getY() - this.viewHeight - 78 - 4;
            } else if (obj instanceof SlotFakeCraftingMatrix s) {
                s.yDisplayPosition = this.ySize + s.getY() - this.viewHeight - 78 - 4;
            } else if (obj instanceof SlotPatternTerm s) {
                s.yDisplayPosition = this.ySize + s.getY() - this.viewHeight - 78 - 4;
            } else if (obj instanceof final AppEngSlot slot) {
                slot.yDisplayPosition = this.ySize + slot.getY() - 78 - 4;
            }
        }
    }

    protected boolean isPowered() {
        return ((ContainerWirelessDualInterfaceTerminal) this.inventorySlots).hasPower;
    }

    @Override
    public AEBaseGui getAEBaseGui() {
        return this;
    }

    @Override
    public float getzLevel() {
        return this.zLevel;
    }

    @Override
    public void postStackUpdate(List<? extends IAEStack<?>> list) {
        for (IAEBasePanel panel : this.panels) {
            if (panel.isActive() && panel instanceof IGuiMonitor monitor) {
                monitor.postStackUpdate(list);
            }
        }
        for (IAEStack<?> stack : list) {
            if (stack instanceof IAEItemStack item && this.blankPatternView.equals(item)) this.blankPatternView = item;
        }
    }

    @Override
    public void setScrollBar() {
        for (IAEBasePanel panel : this.panels) {
            if (panel.isActive() && panel instanceof IGuiMonitor monitor) {
                monitor.setScrollBar();
            }
        }
    }

    @Override
    public AdvItemRepo getRepo() {
        return this.itemPanel.getRepo();
    }

    @Override
    public void setPlayerInv(ItemStack is) {
        this.itemPanel.setPlayerInv(is);

    }

    @Override
    public THGuiTextField getSearchField() {
        return this.itemPanel.getSearchField();
    }

    @Override
    public Enum<?> getSortBy() {
        return this.configSrc.getSetting(Settings.SORT_BY);
    }

    @Override
    public Enum<?> getSortDir() {
        return this.configSrc.getSetting(Settings.SORT_DIRECTION);
    }

    @Override
    public Enum<?> getSortDisplay() {
        return this.configSrc.getSetting(Settings.VIEW_MODE);
    }

    @Override
    public void updateTypeFilters(Reference2BooleanMap<IAEStackType<?>> map) {
        this.itemPanel.typeFilter()
            .setFilters(map);
        this.itemPanel.typeFilter()
            .syncButtonState();
        this.itemPanel.getRepo()
            .updateView();
    }

    @Override
    public void updateFlowRates(Map<IAEStack<?>, FlowRate> rates) {
        this.itemPanel.updateFlowRates(rates);
    }

    @Override
    public void updateSetting(IConfigManager manager, Enum settingName, Enum newValue) {
        for (IAEBasePanel panel : this.panels) {
            if (panel.isActive() && panel instanceof IConfigManagerHost host) {
                host.updateSetting(manager, settingName, newValue);
            }
        }
    }

    @Override
    public void handleKeyboardInput() {
        super.handleKeyboardInput();
        if (this.itemPanel != null) {
            this.itemPanel.handleKeyboardInput();
        }
    }
}
