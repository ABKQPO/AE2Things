package com.asdflj.ae2thing.client.gui;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.client.gui.container.ContainerMonitor;
import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.client.gui.container.slot.SlotPatternFake;
import com.asdflj.ae2thing.client.gui.widget.DualPatternSlot;
import com.asdflj.ae2thing.client.gui.widget.DualTerminalComponentTree;
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
import com.glodblock.github.common.item.ItemFluidDrop;

import appeng.api.config.Settings;
import appeng.api.parts.IPatternTerminal;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.util.IConfigManager;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.slots.VirtualMEMonitorableSlot;
import appeng.client.gui.slots.VirtualMEPatternSlot;
import appeng.client.gui.slots.VirtualMESlot;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.gui.widgets.IDropToFillTextField;
import appeng.client.gui.widgets.ITooltip;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.container.slot.SlotPatternTerm;
import appeng.container.slot.SlotRestrictedInput;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.me.cache.ItemFlowGridCache.FlowRate;
import appeng.util.IConfigManagerHost;
import codechicken.nei.item.ItemFluidDisplay;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;

public class GuiWirelessDualInterfaceTerminal extends GuiBaseInterfaceWireless implements IWidgetGui, IGuiDrawSlot,
    IGuiMonitorTerminal, IConfigManagerHost, IGuiSelection, IDropToFillTextField, ITypeFilterGui, IFlowRateGui {

    private static final int PATTERN_DRAG_REGION_HEIGHT = 93;
    private static final int PATTERN_VIRTUAL_SLOTS = 32;

    public ContainerWirelessDualInterfaceTerminal container;
    private GuiTabButton craftingStatusBtn;
    private PanelDragButton patternDragButton;
    private PanelDragButton itemDragButton;
    private final int baseXSize;
    private final DualTerminalComponentTree componentTree = new DualTerminalComponentTree();
    private IAEBasePanel activePanel = null;
    private IAEBasePanel pointerPanel = null;
    private Point mouse;
    private boolean dragging = false;
    private boolean drawingPanelSlots;
    private int lastMouseX;
    private int lastMouseY;
    private boolean panelPositionsDirty;
    private boolean panelPositionsInitialized;
    private final ItemPanel itemPanel;
    private final PatternPanel patternPanel;
    private final List<DualTerminalComponentTree.Component> itemSlotComponents = new ArrayList<>();
    private VirtualMESlot renderedVirtualSlotUnderMouse;
    private DualPatternSlot[] patternInputSlots;
    private DualPatternSlot[] patternOutputSlots;
    private IAEBasePanel tooltipSourcePanel;
    private List<?> deferredPanelTooltip;
    private int deferredPanelTooltipX;
    private int deferredPanelTooltipY;
    private FontRenderer deferredPanelTooltipFont;
    private String deferredButtonTooltip;
    private int deferredButtonTooltipX;
    private int deferredButtonTooltipY;
    private boolean deferredButtonTooltipFromPanel;
    private boolean renderingDeferredButtonTooltip;
    private IAEItemStack blankPatternView = IPatternTerminal.createBlankPattern()
        .setStackSize(0);

    public GuiWirelessDualInterfaceTerminal(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(inventoryPlayer, te);
        container = (ContainerWirelessDualInterfaceTerminal) this.inventorySlots;
        this.itemPanel = new ItemPanel(this, container, this.configSrc);
        this.patternPanel = new PatternPanel(this, container);
        this.addPanelComponents();
        ((ContainerMonitor) this.inventorySlots).setGui(this);
        this.baseXSize = this.xSize;
    }

    private void addPanelComponents() {
        DualTerminalComponentTree.Panel pattern = this.componentTree.addPanel(this.patternPanel)
            .setRootBehavior(false, false);
        for (IDraggable.Rectangle region : this.patternPanel.getVisualRegions()) {
            pattern.addComponent(region.x(), region.y(), region.width(), region.height());
        }

        DualTerminalComponentTree.Panel items = this.componentTree.addPanel(this.itemPanel)
            .setRootBehavior(false, false);
        DualTerminalComponentTree.Component itemFrame = items.addComponent(0, 0, 101, 96);
        itemFrame.addChild(3, 4, 72, 12)
            .setBlocksNEI(false)
            .setDraggable(false)
            .setFocusedWhen(this.itemPanel::isSearchFocused);
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                this.itemSlotComponents.add(
                    itemFrame.addChild(5 + column * 18, 18 + row * 18, 18, 18)
                        .setBlocksNEI(false)
                        .setDraggable(false));
            }
        }
        items.addComponent(-18, 0, 18, 80)
            .setBlocksNEI(false)
            .setDraggable(false);
        items.addComponent(-36, 0, 18, 96)
            .setBlocksNEI(false)
            .setDraggable(false);
    }

    private void bindItemSlotsToComponents() {
        for (DualTerminalComponentTree.Component component : this.itemSlotComponents) {
            component.clearSlots();
        }
        List<VirtualMEMonitorableSlot> slots = this.itemPanel.getComponentSlots();
        for (int i = 0; i < Math.min(slots.size(), this.itemSlotComponents.size()); i++) {
            this.itemSlotComponents.get(i)
                .bindSlot(slots.get(i));
        }
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
    }

    @Override
    protected void drawVirtualSlotTooltips(int mouseX, int mouseY) {
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(-this.guiLeft, -this.guiTop, 0.0F);
            this.drawPanelComponents(this.lastMouseX, this.lastMouseY);
        } finally {
            GL11.glPopMatrix();
        }

        if (this.renderedVirtualSlotUnderMouse != null) {
            final IAEBasePanel topPanel = this.componentTree.findTopPanel(this.lastMouseX, this.lastMouseY);
            if (topPanel == this.itemPanel || topPanel == this.patternPanel) {
                this.tooltipSourcePanel = topPanel;
                try {
                    super.drawVirtualSlotTooltips(mouseX, mouseY);
                } finally {
                    this.tooltipSourcePanel = null;
                }
            }
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
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
        IAEBasePanel panel = this.componentTree.findTopPanel(mousex, mousey);
        if (panel == null) panel = this.findPanelButtonAt(mousex, mousey);
        if (panel != null) {
            if (panel == this.itemPanel) this.itemPanel.setTextFieldValue(displayName, mousex, mousey, stack);
            return;
        }
        super.setTextFieldValue(displayName, mousex, mousey, stack);
    }

    @Override
    protected boolean capturesMouseAt(int mouseX, int mouseY) {
        return this.findInputPanelAt(mouseX, mouseY) != null;
    }

    private IAEBasePanel findInputPanelAt(int mouseX, int mouseY) {
        IAEBasePanel panel = this.componentTree.findTopPanel(mouseX, mouseY);
        return panel != null ? panel : this.findPanelButtonAt(mouseX, mouseY);
    }

    public boolean suppressCoveredSlotTooltip(int mouseX, int mouseY, ItemStack hoveredStack) {
        IAEBasePanel panel = this.componentTree.findTopPanel(mouseX, mouseY);
        if (panel == this.patternPanel) {
            final DualPatternSlot virtualSlot = this.findPatternPanelSlotAt(mouseX, mouseY);
            if (virtualSlot != null) return virtualSlot.getAEStack() == null;
            Slot slot = super.getSlot(mouseX, mouseY);
            return !this.isPatternSlot(slot);
        }
        if (panel == this.itemPanel) {
            VirtualMEMonitorableSlot slot = this.findItemPanelSlotAt(mouseX, mouseY);
            return slot == null || slot.getAEStack() == null;
        }
        return panel != null || this.findPanelButtonAt(mouseX, mouseY) != null;
    }

    private VirtualMEMonitorableSlot findItemPanelSlotAt(int mouseX, int mouseY) {
        for (VirtualMEMonitorableSlot slot : this.itemPanel.getComponentSlots()) {
            if (slot.isHidden()) continue;
            int x = this.guiLeft + slot.getX();
            int y = this.guiTop + slot.getY();
            DualTerminalComponentTree.Component owner = this.componentTree.getSlotOwner(slot);
            if (mouseX >= x && mouseX < x + 18
                && mouseY >= y
                && mouseY < y + 18
                && this.componentTree.isTopComponent(owner, mouseX, mouseY)) return slot;
        }
        return null;
    }

    private DualPatternSlot findPatternPanelSlotAt(int mouseX, int mouseY) {
        if (this.patternInputSlots == null) return null;
        if (this.componentTree.findTopPanel(mouseX, mouseY) != this.patternPanel) return null;
        for (int i = 0; i < this.patternInputSlots.length; i++) {
            DualPatternSlot slot = this.findPatternSlotAt(this.patternInputSlots[i], mouseX, mouseY);
            if (slot == null) slot = this.findPatternSlotAt(this.patternOutputSlots[i], mouseX, mouseY);
            if (slot != null) return slot;
        }
        return null;
    }

    private DualPatternSlot findPatternSlotAt(DualPatternSlot slot, int mouseX, int mouseY) {
        if (slot.isHidden()) return null;
        int x = this.guiLeft + slot.getX();
        int y = this.guiTop + slot.getY();
        return mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18 ? slot : null;
    }

    public boolean isFloatingComponentAt(int mouseX, int mouseY) {
        return this.componentTree.findTopPanel(mouseX, mouseY) != null
            || this.findPanelButtonAt(mouseX, mouseY) != null;
    }

    public boolean isFloatingComponentAtLastMouse() {
        return this.isFloatingComponentAt(this.lastMouseX, this.lastMouseY);
    }

    public ItemStack getVisibleStackAtLastMouse() {
        return this.getVisibleStackAt(this.lastMouseX, this.lastMouseY);
    }

    public ItemStack getVisibleStackAt(int mouseX, int mouseY) {
        IAEBasePanel panel = this.componentTree.findTopPanel(mouseX, mouseY);
        if (panel == this.patternPanel) {
            DualPatternSlot virtualSlot = this.findPatternPanelSlotAt(mouseX, mouseY);
            if (virtualSlot != null) {
                return this.getItemStackForTooltip(virtualSlot.getAEStack());
            }
            Slot slot = super.getSlot(mouseX, mouseY);
            return this.isPatternSlot(slot) ? slot.getStack() : null;
        }
        if (panel == this.itemPanel) {
            VirtualMEMonitorableSlot slot = this.findItemPanelSlotAt(mouseX, mouseY);
            return slot == null ? null : this.getItemStackForTooltip(slot.getAEStack());
        }
        return null;
    }

    private ItemStack getItemStackForTooltip(IAEStack<?> stack) {
        if (stack instanceof IAEItemStack itemStack) return itemStack.getItemStack();
        if (stack instanceof IAEFluidStack fluidStack && fluidStack.getFluidStack() != null) {
            final ItemStack display = ItemFluidDisplay.createStack(
                fluidStack.getFluidStack()
                    .getFluid(),
                stack.getStackSize());
            if (display != null) return display;
            return ItemFluidDrop.newDisplayStack(fluidStack.getFluidStack());
        }
        return null;
    }

    public boolean isVirtualSlotVisible(VirtualMESlot slot) {
        if (slot.isHidden()) return false;
        if (slot instanceof DualPatternSlot) {
            int x = this.guiLeft + slot.getX() + 9;
            int y = this.guiTop + slot.getY() + 9;
            return this.componentTree.findTopPanel(x, y) == this.patternPanel;
        }
        if (!(slot instanceof VirtualMEMonitorableSlot)) return false;
        DualTerminalComponentTree.Component owner = this.componentTree.getSlotOwner(slot);
        int x = this.guiLeft + slot.getX() + 9;
        int y = this.guiTop + slot.getY() + 9;
        return owner != null && this.componentTree.isTopComponent(owner, x, y);
    }

    private boolean isNativeSlotVisibleAt(Slot slot, int mouseX, int mouseY) {
        if (slot == null) return false;
        IAEBasePanel topPanel = this.componentTree.findTopPanel(mouseX, mouseY);
        return topPanel == null || topPanel == this.patternPanel && this.isPatternSlot(slot);
    }

    @Override
    protected boolean func_146978_c(int left, int top, int width, int height, int mouseX, int mouseY) {
        if (width == 16 && height == 16) {
            boolean foundSlot = false;
            for (Object object : this.inventorySlots.inventorySlots) {
                if (!(object instanceof Slot slot) || slot.xDisplayPosition != left || slot.yDisplayPosition != top) {
                    continue;
                }
                foundSlot = true;
                if (this.isNativeSlotVisibleAt(slot, mouseX, mouseY)) {
                    return super.func_146978_c(left, top, width, height, mouseX, mouseY);
                }
            }
            if (foundSlot) return false;
        }
        return super.func_146978_c(left, top, width, height, mouseX, mouseY);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float btn) {
        this.xSize = baseXSize;
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.deferredPanelTooltip = null;
        this.deferredButtonTooltip = null;
        this.deferredButtonTooltipFromPanel = false;
        this.renderedVirtualSlotUnderMouse = null;
        this.orderPanelButtons();
        if (dragging) {
            this.moveActivePanel();
        }
        this.updateDragButtons();
        for (IAEBasePanel panel : this.componentTree.getPanels()) {
            if (panel.isActive()) {
                panel.drawScreen(mouseX, mouseY, btn);
            }
        }
        this.orderPanelButtons();
        this.updatePanelSlots();
        if (this.itemPanel.getRepo()
            .hasCache()) {
            try {
                this.itemPanel.getRepo()
                    .getLock()
                    .lock();
                super.drawScreen(mouseX, mouseY, btn);
                this.clearOccludedHoveredSlot(mouseX, mouseY);
            } finally {
                this.itemPanel.getRepo()
                    .getLock()
                    .unlock();
            }
        } else {
            super.drawScreen(mouseX, mouseY, btn);
            this.clearOccludedHoveredSlot(mouseX, mouseY);
        }
        this.drawDeferredTooltips();
    }

    private void clearOccludedHoveredSlot(int mouseX, int mouseY) {
        IAEBasePanel panel = this.componentTree.findTopPanel(mouseX, mouseY);
        Slot slot = super.getSlot(mouseX, mouseY);
        if (panel != null && (panel != this.patternPanel || !this.isPatternSlot(slot))) {
            this.theSlot = null;
        }
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        this.lastMouseX = xCoord;
        this.lastMouseY = yCoord;
        final DualPatternSlot patternVirtualSlot = this.findPatternPanelSlotAt(xCoord, yCoord);
        if (patternVirtualSlot != null) {
            this.pointerPanel = this.patternPanel;
            this.bringToFront(this.patternPanel);
            this.renderedVirtualSlotUnderMouse = patternVirtualSlot;
        }
        this.pointerPanel = this.findInputPanelAt(xCoord, yCoord);
        if (this.pointerPanel != this.itemPanel) this.itemPanel.clearSearchFocus();
        if (this.pointerPanel != null) {
            this.bringToFront(this.pointerPanel);
            this.clearBaseSearchFocus();
            if (btn == 0 && this.pointerPanel.draggable() && this.isDragButtonAt(this.pointerPanel, xCoord, yCoord)) {
                this.beginDrag(this.pointerPanel, xCoord, yCoord);
                return;
            }
            this.pointerPanel.mouseClicked(xCoord, yCoord, btn);
        }
        super.mouseClicked(xCoord, yCoord, btn);
    }

    private boolean isInteractiveAt(IAEBasePanel panel, int x, int y) {
        if (panel == this.itemPanel && this.itemPanel.isOverTextField(x, y)) return true;
        for (GuiButton button : this.buttonList) {
            if (this.ownsPanelButton(panel, button) && button.visible
                && x >= button.xPosition
                && x < button.xPosition + button.width
                && y >= button.yPosition
                && y < button.yPosition + button.height) return true;
        }
        Slot slot = super.getSlot(x, y);
        if (slot != null && panel == this.patternPanel && this.isPatternSlot(slot)) return true;
        if (panel == this.patternPanel && this.findPatternPanelSlotAt(x, y) != null) return true;
        if (panel == this.itemPanel && this.findItemPanelSlotAt(x, y) != null) return true;
        return panel == this.patternPanel && this.patternPanel.isInteractiveAt(x, y);
    }

    @Override
    protected void mouseClickMove(final int x, final int y, final int c, final long d) {
        this.lastMouseX = x;
        this.lastMouseY = y;
        if (this.dragging) {
            this.moveActivePanel();
            return;
        }
        if (this.pointerPanel == null) this.pointerPanel = this.findInputPanelAt(x, y);
        if (this.pointerPanel != null) {
            this.pointerPanel.mouseClickMove(x, y, c, d);
        } else {
            for (IAEBasePanel panel : this.componentTree.getPanels()) {
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
        boolean mouseReleased = Mouse.getEventButton() != -1 && !Mouse.getEventButtonState();
        boolean leftMouseReleased = mouseReleased && Mouse.getEventButton() == 0;
        if (leftMouseReleased && this.dragging) {
            this.sendPanelPositions();
        }
        try {
            super.handleMouseInput();
        } finally {
            if (leftMouseReleased) {
                this.activePanel = null;
                this.mouse = null;
                this.dragging = false;
            }
            if (mouseReleased) this.pointerPanel = null;
        }
    }

    @Override
    protected void handleMouseClick(Slot slot, int slotIdx, int ctrlDown, int mouseButton) {
        if (slot != null) slotIdx = slot.slotNumber;
        IAEBasePanel inputPanel = this.pointerPanel != null ? this.pointerPanel
            : this.findInputPanelAt(this.lastMouseX, this.lastMouseY);
        if (inputPanel != null) {
            if (inputPanel.handleMouseClick(slot, slotIdx, ctrlDown, mouseButton)) return;
            if (inputPanel instanceof PatternPanel && this.isPatternSlot(slot)) {
                super.handleMouseClick(slot, slotIdx, ctrlDown, mouseButton);
            }
            return;
        }
        if (slot instanceof AppEngSlot appEngSlot && appEngSlot.isPlayerSide()) {
            super.handleMouseClick(slot, slotIdx, ctrlDown, mouseButton);
            return;
        }
        List<IAEBasePanel> panels = this.componentTree.getPanels();
        for (int index = panels.size() - 1; index >= 0; index--) {
            IAEBasePanel panel = panels.get(index);
            if (!panel.isActive()) continue;
            if (panel.handleMouseClick(slot, slotIdx, ctrlDown, mouseButton)) return;
        }
        if (slotIdx < 0) return;
        super.handleMouseClick(slot, slotIdx, ctrlDown, mouseButton);
    }

    @Override
    protected boolean handleVirtualSlotClick(VirtualMESlot slot, int mouseButton) {
        IAEBasePanel inputPanel = this.pointerPanel != null ? this.pointerPanel
            : this.findInputPanelAt(this.lastMouseX, this.lastMouseY);
        if (inputPanel != null && inputPanel.handleVirtualSlotClick(slot, mouseButton)) {
            return true;
        }
        List<IAEBasePanel> panels = this.componentTree.getPanels();
        for (int index = panels.size() - 1; index >= 0; index--) {
            IAEBasePanel panel = panels.get(index);
            if (!panel.isActive()) continue;
            if (panel.handleVirtualSlotClick(slot, mouseButton)) return true;
        }
        return super.handleVirtualSlotClick(slot, mouseButton);
    }

    @Override
    protected boolean mouseWheelEvent(int mouseX, int mouseY, int wheel) {
        IAEBasePanel topPanel = this.componentTree.findTopPanel(mouseX, mouseY);
        if (topPanel != null) {
            this.bringToFront(topPanel);
            topPanel.mouseWheelEvent(mouseX, mouseY, wheel);
            return true;
        }
        List<IAEBasePanel> panels = this.componentTree.getPanels();
        for (int index = panels.size() - 1; index >= 0; index--) {
            IAEBasePanel panel = panels.get(index);
            if (!panel.isActive()) continue;
            if (panel.mouseWheelEvent(mouseX, mouseY, wheel)) return true;
        }
        return super.mouseWheelEvent(mouseX, mouseY, wheel);
    }

    @Override
    protected void keyTyped(char character, int key) {
        this.xSize = baseXSize;
        IAEBasePanel focusedPanel = this.componentTree.getKeyboardFocusedPanel();
        if (key == Keyboard.KEY_ESCAPE) {
            if (focusedPanel != null) focusedPanel.keyTyped(character, key);
            super.keyTyped(character, key);
            return;
        }

        List<IAEBasePanel> panels = this.componentTree.getPanels();
        for (int index = panels.size() - 1; index >= 0; index--) {
            IAEBasePanel panel = panels.get(index);
            if (!panel.isActive()) continue;
            if (focusedPanel != null && panel != focusedPanel) continue;
            if (panel.keyTyped(character, key)) return;
        }
        super.keyTyped(character, key);
    }

    @Override
    public void func_146977_a(final Slot s) {
        int slotX = this.guiLeft + s.xDisplayPosition + 8;
        int slotY = this.guiTop + s.yDisplayPosition + 8;
        IAEBasePanel occludingPanel = this.componentTree.findTopPanel(slotX, slotY);
        if (!this.drawingPanelSlots && occludingPanel != null) return;
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
        IAEBasePanel panel = this.componentTree.findTopPanel(mouseX, mouseY);
        Slot hoveredSlot = super.getSlot(mouseX, mouseY);
        if (panel == this.patternPanel) {
            if (this.findPatternPanelSlotAt(mouseX, mouseY) == null && !this.isPatternSlot(hoveredSlot)) {
                return new ArrayList<>();
            }
        } else if (panel == this.itemPanel) {
            VirtualMEMonitorableSlot virtualSlot = this.findItemPanelSlotAt(mouseX, mouseY);
            if (virtualSlot == null || virtualSlot.getAEStack() == null) return new ArrayList<>();
        } else if (panel != null || this.findPanelButtonAt(mouseX, mouseY) != null) {
            return new ArrayList<>();
        }
        super.handleItemTooltip(stack, mouseX, mouseY, lines);
        if (this.renderedVirtualSlotUnderMouse instanceof VirtualMEPatternSlot patternSlot) {
            final IAEStack<?> stackInSlot = patternSlot.getAEStack();
            if (stackInSlot != null) {
                lines.add(ButtonToolTips.ChangeAmount.getLocal());
            }
            if (stackInSlot instanceof IAEItemStack) {
                lines.add(ButtonToolTips.RenameItem.getLocal());
            }
        }
        Slot input = this.container.getContainer()
            .getPatternInputSlot();
        if (this.getSlot(mouseX, mouseY) == input && !input.getHasStack()) {
            lines.add(GuiText.BlankPatternInNetwork.getLocal());
            lines.add(String.format(ButtonToolTips.ItemsStored.getLocal(), this.blankPatternView.getStackSize()));
        }
        return lines;
    }

    @Override
    public void renderToolTip(ItemStack stack, int mouseX, int mouseY) {
        IAEBasePanel panel = this.componentTree.findTopPanel(mouseX, mouseY);
        Slot hoveredSlot = super.getSlot(mouseX, mouseY);
        if (panel != null && (panel != this.patternPanel
            || this.findPatternPanelSlotAt(mouseX, mouseY) == null && !this.isPatternSlot(hoveredSlot))) return;
        super.renderToolTip(stack, mouseX, mouseY);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void drawHoveringText(List textLines, int x, int y, FontRenderer font) {
        if (this.renderingDeferredButtonTooltip) {
            super.drawHoveringText(textLines, x, y, font);
            return;
        }
        IAEBasePanel topPanel = this.componentTree.findTopPanel(this.lastMouseX, this.lastMouseY);
        if (this.tooltipSourcePanel != null && topPanel != this.tooltipSourcePanel) return;
        if (this.renderedVirtualSlotUnderMouse != null && topPanel != this.itemPanel && topPanel != this.patternPanel)
            return;
        IAEBasePanel pointerPanel = this.componentTree.findTopPanel(this.lastMouseX, this.lastMouseY);
        if (pointerPanel == null) pointerPanel = this.findPanelButtonAt(this.lastMouseX, this.lastMouseY);
        boolean panelTooltip = pointerPanel != null
            && this.ownsTooltipAt(pointerPanel, this.lastMouseX, this.lastMouseY);
        if (panelTooltip) {
            this.deferredPanelTooltip = new ArrayList<>(textLines);
            this.deferredPanelTooltipX = x;
            this.deferredPanelTooltipY = y;
            this.deferredPanelTooltipFont = font;
            return;
        }
        if (pointerPanel != null) return;
        this.deferredPanelTooltip = new ArrayList<>(textLines);
        this.deferredPanelTooltipX = x;
        this.deferredPanelTooltipY = y;
        this.deferredPanelTooltipFont = font;
    }

    @Override
    protected void handleTooltip(int mouseX, int mouseY, ITooltip tooltip) {
        if (this.findPanelButtonAt(mouseX, mouseY) != null
            && (!(tooltip instanceof GuiButton button) || !this.isPanelButton(button))) return;
        super.handleTooltip(mouseX, mouseY, tooltip);
    }

    @Override
    public void drawTooltip(int x, int y, String message) {
        if (this.renderingDeferredButtonTooltip) {
            super.drawTooltip(x, y, message);
            return;
        }

        this.deferButtonTooltip(x, y, message);
    }

    @Override
    public void drawTooltip(int x, int y, String[] lines) {
        if (this.renderingDeferredButtonTooltip) {
            super.drawTooltip(x, y, lines);
            return;
        }

        this.deferButtonTooltip(x, y, String.join("\n", lines));
    }

    private void deferButtonTooltip(int x, int y, String message) {
        IAEBasePanel hoveredPanel = this.componentTree.findTopPanel(this.lastMouseX, this.lastMouseY);
        IAEBasePanel buttonPanel = this.findPanelButtonAt(this.lastMouseX, this.lastMouseY);
        boolean panelButtonTooltip = this.isPanelButtonTooltipOrigin(x, y);
        if (this.deferredButtonTooltip != null && this.deferredButtonTooltipFromPanel && !panelButtonTooltip) return;
        if (hoveredPanel == null) hoveredPanel = buttonPanel;
        if (this.tooltipSourcePanel != null) {
            if (hoveredPanel != this.tooltipSourcePanel) return;
            this.deferredButtonTooltip = message;
            this.deferredButtonTooltipX = x;
            this.deferredButtonTooltipY = y;
            this.deferredButtonTooltipFromPanel = panelButtonTooltip;
            return;
        }
        boolean panelOwnsTooltip = buttonPanel == hoveredPanel
            || hoveredPanel == this.itemPanel && this.itemPanel.isOverTextField(this.lastMouseX, this.lastMouseY);
        if (hoveredPanel != null && !panelOwnsTooltip) return;

        this.deferredButtonTooltip = message;
        this.deferredButtonTooltipX = x;
        this.deferredButtonTooltipY = y;
        this.deferredButtonTooltipFromPanel = panelButtonTooltip;
    }

    private boolean isPanelButtonTooltipOrigin(int x, int y) {
        for (GuiButton button : this.buttonList) {
            if (!button.visible || !this.isPanelButton(button)) continue;
            if (x == button.xPosition + 11 && y == Math.max(button.yPosition, 15) + 4) return true;
        }
        return false;
    }

    private boolean isPanelButton(GuiButton button) {
        return this.ownsPanelButton(this.patternPanel, button) || this.ownsPanelButton(this.itemPanel, button);
    }

    private void drawPanelComponents(int mouseX, int mouseY) {
        this.updateDragButtons();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        for (IAEBasePanel panel : this.componentTree.getPanels()) {
            if (!panel.isActive()) continue;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glPushMatrix();
            try {
                this.preparePanelOverlay();
                panel.drawBG(this.guiLeft, this.guiTop, mouseX, mouseY);
                if (panel == this.itemPanel) this.itemPanel.drawComponentForeground();
                if (panel == this.patternPanel) {
                    this.drawPatternSlots(mouseX, mouseY);
                    this.drawPatternSlotsVirtual(mouseX, mouseY);
                }
                if (panel == this.itemPanel) this.drawItemPanelSlots(mouseX, mouseY);

                this.preparePanelOverlay();
                GL11.glPushMatrix();
                try {
                    GL11.glTranslatef(this.guiLeft, this.guiTop, 0.0F);
                    panel.drawFG(this.guiLeft, this.guiTop, mouseX, mouseY);
                } finally {
                    GL11.glMatrixMode(GL11.GL_MODELVIEW);
                    GL11.glPopMatrix();
                }

                this.preparePanelOverlay();
                for (GuiButton button : this.buttonList) {
                    if (this.ownsPanelButton(panel, button)) button.drawButton(this.mc, mouseX, mouseY);
                }
            } finally {
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
                GL11.glPopAttrib();
                this.resetPanelColor();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
            }
        }
    }

    private void drawDeferredTooltips() {
        if (this.deferredButtonTooltip == null && this.deferredPanelTooltip == null) return;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        float previousGuiZ = this.zLevel;
        float previousItemZ = this.itemRender.zLevel;
        try {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.zLevel = 300.0F;
            this.itemRender.zLevel = 300.0F;

            if (this.deferredPanelTooltip != null) {
                super.drawHoveringText(
                    this.deferredPanelTooltip,
                    this.deferredPanelTooltipX,
                    this.deferredPanelTooltipY,
                    this.deferredPanelTooltipFont);
            }
            if (this.deferredButtonTooltip != null) {
                this.renderingDeferredButtonTooltip = true;
                try {
                    super.drawTooltip(
                        this.deferredButtonTooltipX,
                        this.deferredButtonTooltipY,
                        this.deferredButtonTooltip);
                } finally {
                    this.renderingDeferredButtonTooltip = false;
                }
            }
        } finally {
            this.zLevel = previousGuiZ;
            this.itemRender.zLevel = previousItemZ;
            this.deferredButtonTooltip = null;
            this.deferredButtonTooltipFromPanel = false;
            this.deferredPanelTooltip = null;
            this.deferredPanelTooltipFont = null;
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private void preparePanelOverlay() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_ALWAYS);
        GL11.glDepthMask(false);
        this.resetPanelColor();
    }

    private void initPatternSlots() {
        if (this.container.inputsSync == null || this.container.outputsSync == null) return;
        this.patternPanel.setSlotRebaseY(this.ySize - this.viewHeight - 78 - 4);
        if (this.patternInputSlots != null) {
            this.patternPanel.setVirtualSlots(this.patternInputSlots, this.patternOutputSlots);
            return;
        }
        this.patternInputSlots = new DualPatternSlot[PATTERN_VIRTUAL_SLOTS];
        this.patternOutputSlots = new DualPatternSlot[PATTERN_VIRTUAL_SLOTS];
        for (int i = 0; i < PATTERN_VIRTUAL_SLOTS; i++) {
            this.patternInputSlots[i] = new DualPatternSlot(0, 0, this.container.inputsSync, i, this.container);
            this.patternOutputSlots[i] = new DualPatternSlot(0, 0, this.container.outputsSync, i, this.container);
            this.registerVirtualSlots(this.patternInputSlots[i]);
            this.registerVirtualSlots(this.patternOutputSlots[i]);
        }
        this.patternPanel.setVirtualSlots(this.patternInputSlots, this.patternOutputSlots);
    }

    private void drawPatternSlotsVirtual(int mouseX, int mouseY) {
        if (this.patternInputSlots == null) return;
        this.patternPanel.updateSlotPositions();
        final boolean patternHovered = this.componentTree.findTopPanel(mouseX, mouseY) == this.patternPanel;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_ALWAYS);
        GL11.glDepthMask(false);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslatef(this.guiLeft, this.guiTop, 0.0F);
        try {
            int localMouseX = mouseX - this.guiLeft + 1;
            int localMouseY = mouseY - this.guiTop + 1;
            for (DualPatternSlot slot : this.patternInputSlots) {
                this.drawPatternVirtualSlot(slot, localMouseX, localMouseY, patternHovered);
            }
            for (DualPatternSlot slot : this.patternOutputSlots) {
                this.drawPatternVirtualSlot(slot, localMouseX, localMouseY, patternHovered);
            }
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private void drawPatternVirtualSlot(DualPatternSlot slot, int localMouseX, int localMouseY,
        boolean patternHovered) {
        if (slot.isHidden()) return;
        int centerX = this.guiLeft + slot.getX() + 9;
        int centerY = this.guiTop + slot.getY() + 9;
        if (this.componentTree.findTopPanel(centerX, centerY) != this.patternPanel) return;
        if (slot.drawStackAndOverlay(this.mc, localMouseX, localMouseY) && patternHovered) {
            this.renderedVirtualSlotUnderMouse = slot;
        }
    }

    private void drawPatternSlots(int mouseX, int mouseY) {
        this.drawingPanelSlots = true;
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glTranslatef(this.guiLeft, this.guiTop, 0.0F);
        try {
            for (Object object : this.inventorySlots.inventorySlots) {
                if (!(object instanceof Slot slot) || !this.isPatternSlot(slot) || slot.xDisplayPosition <= -8000) {
                    continue;
                }
                if (slot instanceof SlotPatternFake fake && fake.isHidden()) continue;
                int centerX = this.guiLeft + slot.xDisplayPosition + 8;
                int centerY = this.guiTop + slot.yDisplayPosition + 8;
                if (this.componentTree.findTopPanel(centerX, centerY) == this.patternPanel) {
                    this.func_146977_a(slot);
                    int screenX = this.guiLeft + slot.xDisplayPosition;
                    int screenY = this.guiTop + slot.yDisplayPosition;
                    if (mouseX >= screenX && mouseX < screenX + 16 && mouseY >= screenY && mouseY < screenY + 16) {
                        GL11.glDisable(GL11.GL_LIGHTING);
                        GL11.glDepthFunc(GL11.GL_ALWAYS);
                        GL11.glDepthMask(false);
                        drawRect(
                            slot.xDisplayPosition,
                            slot.yDisplayPosition,
                            slot.xDisplayPosition + 16,
                            slot.yDisplayPosition + 16,
                            0x80FFFFFF);
                    }
                }
            }
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            this.drawingPanelSlots = false;
        }
    }

    private void drawItemPanelSlots(int mouseX, int mouseY) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glTranslatef(this.guiLeft, this.guiTop, 0.0F);
        try {
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_ALWAYS);
            GL11.glDepthMask(false);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            int localMouseX = mouseX - this.guiLeft + 1;
            int localMouseY = mouseY - this.guiTop + 1;
            for (VirtualMEMonitorableSlot slot : this.itemPanel.getComponentSlots()) {
                if (slot.isHidden()) continue;
                DualTerminalComponentTree.Component owner = this.componentTree.getSlotOwner(slot);
                int centerX = this.guiLeft + slot.getX() + 9;
                int centerY = this.guiTop + slot.getY() + 9;
                if (!this.componentTree.isTopComponent(owner, centerX, centerY)) continue;
                if (slot.drawStackAndOverlay(this.mc, localMouseX, localMouseY)
                    && this.componentTree.isTopComponent(owner, mouseX, mouseY)) {
                    this.renderedVirtualSlotUnderMouse = slot;
                }
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private boolean ownsTooltipAt(IAEBasePanel panel, int x, int y) {
        if (panel == this.patternPanel) {
            Slot slot = super.getSlot(x, y);
            return this.isPatternSlot(slot) || this.findPanelButtonAt(x, y) == panel;
        }
        if (panel == this.itemPanel) {
            return this.findItemPanelSlotAt(x, y) != null || this.itemPanel.isOverTextField(x, y)
                || this.findPanelButtonAt(x, y) == panel;
        }
        return false;
    }

    private IAEBasePanel findPanelButtonAt(int x, int y) {
        List<IAEBasePanel> panels = this.componentTree.getPanels();
        for (int panelIndex = panels.size() - 1; panelIndex >= 0; panelIndex--) {
            IAEBasePanel panel = panels.get(panelIndex);
            for (GuiButton button : this.buttonList) {
                if (button.visible && this.ownsPanelButton(panel, button)
                    && x >= button.xPosition
                    && x < button.xPosition + button.width
                    && y >= button.yPosition
                    && y < button.yPosition + button.height) {
                    return panel;
                }
            }
        }
        return null;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.xSize = baseXSize;
        super.initGui();
        for (IAEBasePanel panel : this.componentTree.getPanels()) {
            panel.initGui();
        }
        this.bindItemSlotsToComponents();
        this.initPatternSlots();
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
        for (IAEBasePanel panel : this.componentTree.getPanels()) {
            panel.onGuiClosed();
        }
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean hideItemPanelSlot(int x, int y, int w, int h) {
        return this.componentTree.intersectsNEI(x, y, w, h);
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
    public VirtualMESlot getVirtualMESlotUnderMouse() {
        return this.renderedVirtualSlotUnderMouse;
    }

    @Override
    public RenderItem getRenderItem() {
        return itemRender;
    }

    @Override
    public Slot getSlot(int mouseX, int mouseY) {
        Slot slot = super.getSlot(mouseX, mouseY);
        return this.isNativeSlotVisibleAt(slot, mouseX, mouseY) ? slot : null;
    }

    @Override
    protected Slot getSlotAtPosition(int mouseX, int mouseY) {
        Slot slot = super.getSlotAtPosition(mouseX, mouseY);
        return this.isNativeSlotVisibleAt(slot, mouseX, mouseY) ? slot : null;
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
        List<IAEBasePanel> panels = this.componentTree.getPanels();
        for (int index = panels.size() - 1; index >= 0; index--) {
            IAEBasePanel panel = panels.get(index);
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

    private boolean isDragButtonAt(IAEBasePanel panel, int x, int y) {
        PanelDragButton button = this.getPanelDragButton(panel);
        return button != null && button.visible
            && x >= button.xPosition
            && x < button.xPosition + button.width
            && y >= button.yPosition
            && y < button.yPosition + button.height;
    }

    private PanelDragButton getPanelDragButton(IAEBasePanel panel) {
        if (panel == this.patternPanel) return this.patternDragButton;
        if (panel == this.itemPanel) return this.itemDragButton;
        return null;
    }

    private void moveActivePanel() {
        if (!this.dragging || this.activePanel == null || this.mouse == null) return;
        IDraggable.Rectangle rectangle = this.activePanel.getRectangle();
        int x = this.lastMouseX - this.mouse.x;
        int y = this.lastMouseY - this.mouse.y;
        x = this.clampPanelX(x, rectangle.width());
        int dragRegionHeight = this.activePanel == this.patternPanel ? PATTERN_DRAG_REGION_HEIGHT : rectangle.height();
        y = this.clampPanelY(y, dragRegionHeight);
        this.panelPositionsDirty |= x != rectangle.x() || y != rectangle.y();
        this.activePanel.move(x, y);
    }

    private void restorePanelPositions() {
        if (this.panelPositionsInitialized) {
            IDraggable.Rectangle patternRectangle = this.patternPanel.getRectangle();
            IDraggable.Rectangle itemRectangle = this.itemPanel.getRectangle();
            this.patternPanel.setRectangle(
                this.clampPanelX(patternRectangle.x(), patternRectangle.width()),
                this.clampPanelY(patternRectangle.y(), PATTERN_DRAG_REGION_HEIGHT));
            this.itemPanel.setRectangle(
                this.clampPanelX(itemRectangle.x(), itemRectangle.width()),
                this.clampPanelY(itemRectangle.y(), itemRectangle.height()));
            return;
        }
        NBTTagCompound positions = this.container.getPanelPositions();
        IDraggable.Rectangle patternRectangle = this.patternPanel.getRectangle();
        IDraggable.Rectangle itemRectangle = this.itemPanel.getRectangle();
        int patternDefaultX = this.guiLeft + 209;
        int patternDefaultY = this.guiTop;
        int itemDefaultX = this.guiLeft - itemRectangle.width();
        int itemDefaultY = this.guiTop + this.ySize - itemRectangle.height();
        this.patternPanel.setRectangle(
            this.clampPanelX(this.getPanelPosition(positions, "patternX", patternDefaultX), patternRectangle.width()),
            this.clampPanelY(
                this.getPanelPosition(positions, "patternY", patternDefaultY),
                PATTERN_DRAG_REGION_HEIGHT));
        this.itemPanel.setRectangle(
            this.clampPanelX(this.getPanelPosition(positions, "itemX", itemDefaultX), itemRectangle.width()),
            this.clampPanelY(this.getPanelPosition(positions, "itemY", itemDefaultY), itemRectangle.height()));
        this.panelPositionsInitialized = true;
    }

    private int getPanelPosition(NBTTagCompound positions, String key, int defaultValue) {
        return positions.hasKey(key) ? positions.getInteger(key) : defaultValue;
    }

    private void sendPanelPositions() {
        if (!this.panelPositionsDirty) return;
        IDraggable.Rectangle patternRectangle = this.patternPanel.getRectangle();
        IDraggable.Rectangle itemRectangle = this.itemPanel.getRectangle();
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
        int visible = Math.min(16, Math.min(width, this.width));
        return Math.max(visible - width, Math.min(x, this.width - visible));
    }

    private int clampPanelY(int y, int height) {
        int visible = Math.min(16, Math.min(height, this.height));
        return Math.max(visible - height, Math.min(y, this.height - visible));
    }

    private void resetPanelColor() {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void updatePanelSlots() {
        for (IAEBasePanel panel : this.componentTree.getPanels()) {
            if (panel instanceof PatternPanel patternPanel) {
                patternPanel.updateSlotPositions();
            }
        }
    }

    private void updateDragButtons() {
        this.updateDragButton(this.patternDragButton, this.patternPanel);
        this.updateDragButton(this.itemDragButton, this.itemPanel);
    }

    private void updateDragButton(PanelDragButton button, IAEBasePanel panel) {
        if (button == null || panel == null) return;
        IDraggable.Rectangle handle = panel.getDragHandle();
        if (handle.x() + handle.width() > this.width) {
            IDraggable.Rectangle rectangle = panel.getRectangle();
            handle = new IDraggable.Rectangle(rectangle.x(), rectangle.y(), handle.width(), handle.height());
        }
        button.xPosition = handle.x();
        button.yPosition = handle.y();
        button.width = handle.width();
        button.height = handle.height();
        button.visible = panel.isActive();
    }

    private void bringToFront(IAEBasePanel panel) {
        this.componentTree.bringToFront(panel);
        this.orderPanelButtons();
    }

    private void orderPanelButtons() {
        List<GuiButton> panelButtons = new ArrayList<>();
        for (GuiButton button : this.buttonList) {
            for (IAEBasePanel panel : this.componentTree.getPanels()) {
                if (this.ownsPanelButton(panel, button)) {
                    panelButtons.add(button);
                    break;
                }
            }
        }
        this.buttonList.removeAll(panelButtons);
        for (IAEBasePanel panel : this.componentTree.getPanels()) {
            for (GuiButton button : panelButtons) {
                if (this.ownsPanelButton(panel, button)) this.buttonList.add(button);
            }
        }
    }

    private boolean ownsPanelButton(IAEBasePanel panel, GuiButton button) {
        if (panel == this.patternPanel) {
            return button == this.patternDragButton || this.patternPanel.ownsButton(button);
        }
        if (panel == this.itemPanel) {
            return button == this.itemDragButton || this.itemPanel.ownsButton(button);
        }
        return false;
    }

    private boolean isPatternSlot(Slot slot) {
        return slot instanceof SlotPatternFake || slot instanceof SlotFakeCraftingMatrix
            || slot instanceof SlotPatternTerm
            || slot instanceof SlotRestrictedInput;
    }

    @Override
    protected void repositionSlots() {
        for (final Object obj : this.inventorySlots.inventorySlots) {
            if (obj instanceof SlotPatternFake s) {
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
        for (IAEBasePanel panel : this.componentTree.getPanels()) {
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
        for (IAEBasePanel panel : this.componentTree.getPanels()) {
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
        for (IAEBasePanel panel : this.componentTree.getPanels()) {
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
