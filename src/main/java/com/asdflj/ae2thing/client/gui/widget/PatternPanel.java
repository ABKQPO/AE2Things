package com.asdflj.ae2thing.client.gui.widget;

import static com.asdflj.ae2thing.api.Constants.MODE_CRAFTING;
import static com.asdflj.ae2thing.api.Constants.MODE_PROCESSING;
import static net.minecraft.client.gui.GuiScreen.isCtrlKeyDown;
import static net.minecraft.client.gui.GuiScreen.isShiftKeyDown;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.api.InventoryActionExtend;
import com.asdflj.ae2thing.client.event.EncodeEvent;
import com.asdflj.ae2thing.client.gui.IWidgetGui;
import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.client.gui.container.slot.SlotPattern;
import com.asdflj.ae2thing.client.gui.container.slot.SlotPatternFake;
import com.asdflj.ae2thing.network.CPacketInventoryActionExtend;
import com.asdflj.ae2thing.network.CPacketTerminalBtns;
import com.asdflj.ae2thing.util.Ae2ReflectClient;
import com.asdflj.ae2thing.util.ModAndClassUtil;
import com.asdflj.ae2thing.util.Util;
import com.glodblock.github.client.gui.GuiFCImgButton;

import appeng.api.config.ActionItems;
import appeng.api.config.ItemSubstitution;
import appeng.api.config.PatternBeSubstitution;
import appeng.api.config.PatternSlotConfig;
import appeng.api.config.Settings;
import appeng.api.storage.data.IAEItemStack;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.container.AEBaseContainer;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.SlotFake;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.container.slot.SlotPatternTerm;
import appeng.container.slot.SlotRestrictedInput;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.helpers.InventoryAction;
import appeng.util.item.AEItemStack;
import codechicken.nei.NEIClientUtils;

public class PatternPanel implements IAEBasePanel {

    private final AEBaseGui parent;
    private final IWidgetGui gui;
    private final ContainerWirelessDualInterfaceTerminal container;
    protected GuiImgButton encodeBtn;
    protected GuiImgButton substitutionsEnabledBtn;
    protected GuiImgButton substitutionsDisabledBtn;
    protected GuiFCImgButton fluidPrioritizedEnabledBtn;
    protected GuiFCImgButton fluidPrioritizedDisabledBtn;
    protected GuiImgButton invertBtn;
    protected GuiImgButton clearBtn;
    protected GuiImgButton doubleBtn;
    protected GuiImgButton beSubstitutionsEnabledBtn;
    protected GuiImgButton beSubstitutionsDisabledBtn;
    protected GuiFCImgButton combineEnableBtn;
    protected GuiFCImgButton combineDisableBtn;
    protected GuiTabButton tabProcessButton;
    protected GuiTabButton tabCraftButton;
    protected final GuiScrollbar processingScrollBar = new GuiScrollbar();
    private final AEBaseContainer inventorySlots;
    protected final Set<Slot> drag_click;
    private static final int PANEL_X = 209;
    private static final int PANEL_Y = 0;
    private static final int PANEL_WIDTH = 133;
    private static final int PANEL_HEIGHT = 202;
    private static final int MODE_TAB_X = 39;
    private static final int MODE_TAB_Y = 93;
    private static final int MODE_TAB_SIZE = 22;
    private final List<PanelSection> sections = new ArrayList<>();
    private final List<SlotLayout> slotLayouts = new ArrayList<>();
    private int absX;
    private int absY;
    private boolean initialized;

    public PatternPanel(IWidgetGui gui, ContainerWirelessDualInterfaceTerminal container) {
        this.gui = gui;
        this.container = container;
        this.parent = gui.getGui();
        this.inventorySlots = this.container;
        this.drag_click = Ae2ReflectClient.getDragClick(this.parent);
        this.sections.add(new PanelSection(0, 0, 133, 93, 0, 0));
        this.sections.add(new PanelSection(0, 93, 40, 77, 133, 0));
        this.sections.add(new PanelSection(0, 170, 32, 32, 173, 0));
        processingScrollBar.setHeight(70)
            .setWidth(7)
            .setLeft(6)
            .setRange(0, 1, 1);
        processingScrollBar.setTexture(AE2Thing.MODID, getBackground(), 242, 0);
    }

    @Override
    public String getBackground() {
        if (this.container.isCraftingMode()) {
            return "gui/widget/pattern3.png";
        } else {
            return "gui/widget/pattern.png";
        }
    }

    public List<IDraggable.Rectangle> getVisualRegions() {
        List<IDraggable.Rectangle> regions = new ArrayList<>(this.sections.size() + 1);
        for (PanelSection section : this.sections) {
            regions.add(new IDraggable.Rectangle(section.x, section.y, section.width, section.height));
        }
        regions.add(new IDraggable.Rectangle(MODE_TAB_X, MODE_TAB_Y, MODE_TAB_SIZE, MODE_TAB_SIZE));
        return regions;
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        updateButton(this.substitutionsEnabledBtn, this.container.substitute);
        updateButton(this.substitutionsDisabledBtn, !this.container.substitute);
        updateButton(this.combineEnableBtn, this.container.combine);
        updateButton(this.combineDisableBtn, !this.container.combine);
        updateButton(this.beSubstitutionsEnabledBtn, this.container.beSubstitute);
        updateButton(this.beSubstitutionsDisabledBtn, !this.container.beSubstitute);
        updateButton(this.fluidPrioritizedEnabledBtn, this.container.prioritize);
        updateButton(this.fluidPrioritizedDisabledBtn, !this.container.prioritize);
        updateButton(this.tabCraftButton, this.container.isCraftingMode());
        updateButton(this.tabProcessButton, !this.container.isCraftingMode());
        this.processingScrollBar.setVisible(!this.container.isCraftingMode());
        this.processingScrollBar.draw(this.parent);
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTextureBack(getBackground());
        int patternTextureY = this.container.isCraftingMode() || this.container.inverted ? 0 : 93;
        for (PanelSection section : this.sections) {
            this.parent.drawTexturedModalRect(
                this.absX + section.x,
                this.absY + section.y,
                section.textureX,
                section.textureY + (section.y == 0 ? patternTextureY : 0),
                section.width,
                section.height);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float btn) {
        this.updateFixedButtonPositions();
        if (this.container.isCraftingMode()) {
            substitutionsEnabledBtn.xPosition = this.absX + 82;
            substitutionsEnabledBtn.yPosition = this.absY + 14;

            substitutionsDisabledBtn.xPosition = this.absX + 82;
            substitutionsDisabledBtn.yPosition = this.absY + 14;

            if (beSubstitutionsEnabledBtn != null) {
                beSubstitutionsEnabledBtn.xPosition = this.absX + 82;
                beSubstitutionsEnabledBtn.yPosition = this.absY + 24;
                beSubstitutionsDisabledBtn.xPosition = this.absX + 82;
                beSubstitutionsDisabledBtn.yPosition = this.absY + 24;
            }

            fluidPrioritizedEnabledBtn.xPosition = -9000;
            fluidPrioritizedEnabledBtn.yPosition = -9000;

            fluidPrioritizedDisabledBtn.xPosition = -9000;
            fluidPrioritizedDisabledBtn.yPosition = -9000;

            if (doubleBtn != null) {
                doubleBtn.xPosition = -9000;
                doubleBtn.yPosition = -9000;
            }

            clearBtn.xPosition = this.absX + 72;
            clearBtn.yPosition = this.absY + 14;

            invertBtn.xPosition = -9000;
            invertBtn.yPosition = -9000;

            combineEnableBtn.xPosition = -9000;
            combineEnableBtn.yPosition = -9000;

            combineDisableBtn.xPosition = -9000;
            combineDisableBtn.yPosition = -9000;

        } else {
            final int offset = container.inverted ? 18 * -3 : 0;
            substitutionsEnabledBtn.xPosition = this.absX + 97 + offset;
            substitutionsEnabledBtn.yPosition = this.absY + 10;

            substitutionsDisabledBtn.xPosition = this.absX + 97 + offset;
            substitutionsDisabledBtn.yPosition = this.absY + 10;

            if (beSubstitutionsEnabledBtn != null) {
                beSubstitutionsEnabledBtn.xPosition = this.absX + 97 + offset;
                beSubstitutionsEnabledBtn.yPosition = this.absY + 69;
                beSubstitutionsDisabledBtn.xPosition = this.absX + 97 + offset;
                beSubstitutionsDisabledBtn.yPosition = this.absY + 69;
            }

            fluidPrioritizedEnabledBtn.xPosition = this.absX + 97 + offset;
            fluidPrioritizedEnabledBtn.yPosition = this.absY + 59;

            fluidPrioritizedDisabledBtn.xPosition = this.absX + 97 + offset;
            fluidPrioritizedDisabledBtn.yPosition = this.absY + 59;

            if (doubleBtn != null) {
                doubleBtn.xPosition = this.absX + 97 + offset;
                doubleBtn.yPosition = this.absY + 20;
            }

            clearBtn.xPosition = this.absX + 87 + offset;
            clearBtn.yPosition = this.absY + 10;

            invertBtn.xPosition = this.absX + 87 + offset;
            invertBtn.yPosition = this.absY + 20;

            combineEnableBtn.xPosition = this.absX + 87 + offset;
            combineEnableBtn.yPosition = this.absY + 59;

            combineDisableBtn.xPosition = this.absX + 87 + offset;
            combineDisableBtn.yPosition = this.absY + 59;
            processingScrollBar.setCurrentScroll(container.activePage);
        }
    }

    @Override
    public void initGui() {
        if (!this.initialized) {
            this.absX = this.parent.getGuiLeft() + PANEL_X;
            this.absY = this.parent.getGuiTop() + PANEL_Y;
            this.initialized = true;
        }
        this.captureSlotLayout();
        this.gui.getButtonList()
            .add(
                this.encodeBtn = new GuiImgButton(
                    this.absX + 11,
                    this.absY + 118,
                    Settings.ACTIONS,
                    ActionItems.ENCODE));
        this.gui.getButtonList()
            .add(
                this.tabProcessButton = new GuiTabButton(
                    this.absX + MODE_TAB_X,
                    this.absY + MODE_TAB_Y,
                    new ItemStack(Blocks.furnace),
                    GuiText.ProcessingPattern.getLocal(),
                    this.gui.getRenderItem()));
        this.gui.getButtonList()
            .add(
                this.tabCraftButton = new GuiTabButton(
                    this.absX + MODE_TAB_X,
                    this.absY + MODE_TAB_Y,
                    new ItemStack(Blocks.crafting_table),
                    GuiText.CraftingPattern.getLocal(),
                    this.gui.getRenderItem()));
        this.substitutionsEnabledBtn = new GuiImgButton(
            this.absX + 97,
            this.absY + 10,
            Settings.ACTIONS,
            ItemSubstitution.ENABLED);
        this.substitutionsEnabledBtn.setHalfSize(true);
        this.gui.getButtonList()
            .add(this.substitutionsEnabledBtn);

        this.substitutionsDisabledBtn = new GuiImgButton(
            this.absX + 97,
            this.absY + 10,
            Settings.ACTIONS,
            ItemSubstitution.DISABLED);
        this.substitutionsDisabledBtn.setHalfSize(true);
        this.gui.getButtonList()
            .add(this.substitutionsDisabledBtn);

        this.fluidPrioritizedEnabledBtn = new GuiFCImgButton(this.absX + 97, this.absY + 59, "FORCE_PRIO", "DO_PRIO");
        this.fluidPrioritizedEnabledBtn.setHalfSize(true);
        this.gui.getButtonList()
            .add(this.fluidPrioritizedEnabledBtn);

        this.fluidPrioritizedDisabledBtn = new GuiFCImgButton(this.absX + 97, this.absY + 59, "NOT_PRIO", "DONT_PRIO");
        this.fluidPrioritizedDisabledBtn.setHalfSize(true);
        this.gui.getButtonList()
            .add(this.fluidPrioritizedDisabledBtn);

        this.invertBtn = new GuiImgButton(
            this.absX + 87,
            this.absY + 20,
            Settings.ACTIONS,
            container.inverted ? PatternSlotConfig.C_4_16 : PatternSlotConfig.C_16_4);
        this.invertBtn.setHalfSize(true);
        this.gui.getButtonList()
            .add(this.invertBtn);

        this.clearBtn = new GuiImgButton(this.absX + 87, this.absY + 10, Settings.ACTIONS, ActionItems.CLOSE);
        this.clearBtn.setHalfSize(true);
        this.gui.getButtonList()
            .add(this.clearBtn);

        if (ModAndClassUtil.isDoubleButton) {
            this.doubleBtn = new GuiImgButton(this.absX + 97, this.absY + 20, Settings.ACTIONS, ActionItems.DOUBLE);
            this.doubleBtn.setHalfSize(true);
            this.gui.getButtonList()
                .add(this.doubleBtn);
        }

        this.combineEnableBtn = new GuiFCImgButton(this.absX + 87, this.absY + 59, "FORCE_COMBINE", "DO_COMBINE");
        this.combineEnableBtn.setHalfSize(true);
        this.gui.getButtonList()
            .add(this.combineEnableBtn);

        this.combineDisableBtn = new GuiFCImgButton(this.absX + 87, this.absY + 59, "NOT_COMBINE", "DONT_COMBINE");
        this.combineDisableBtn.setHalfSize(true);
        this.gui.getButtonList()
            .add(this.combineDisableBtn);
        if (ModAndClassUtil.isBeSubstitutionsButton) {
            this.beSubstitutionsEnabledBtn = new GuiImgButton(
                this.absX + 97,
                this.absY + 69,
                Settings.ACTIONS,
                PatternBeSubstitution.ENABLED);
            this.beSubstitutionsEnabledBtn.setHalfSize(true);
            this.gui.getButtonList()
                .add(this.beSubstitutionsEnabledBtn);

            this.beSubstitutionsDisabledBtn = new GuiImgButton(
                this.absX + 97,
                this.absY + 69,
                Settings.ACTIONS,
                PatternBeSubstitution.DISABLED);
            this.beSubstitutionsDisabledBtn.setHalfSize(true);
            this.gui.getButtonList()
                .add(this.beSubstitutionsDisabledBtn);
        }
        this.updateScrollBarPosition();
        this.updateFixedButtonPositions();
    }

    private void updateFixedButtonPositions() {
        if (this.encodeBtn != null) {
            this.encodeBtn.xPosition = this.absX + 11;
            this.encodeBtn.yPosition = this.absY + 118;
        }
        if (this.tabProcessButton != null) {
            this.tabProcessButton.xPosition = this.absX + MODE_TAB_X;
            this.tabProcessButton.yPosition = this.absY + MODE_TAB_Y;
        }
        if (this.tabCraftButton != null) {
            this.tabCraftButton.xPosition = this.absX + MODE_TAB_X;
            this.tabCraftButton.yPosition = this.absY + MODE_TAB_Y;
        }
    }

    protected void updateButton(GuiButton button, boolean vis) {
        if (button != null) {
            button.visible = vis;
        }
    }

    public boolean ownsButton(GuiButton button) {
        return button == this.encodeBtn || button == this.substitutionsEnabledBtn
            || button == this.substitutionsDisabledBtn
            || button == this.fluidPrioritizedEnabledBtn
            || button == this.fluidPrioritizedDisabledBtn
            || button == this.invertBtn
            || button == this.clearBtn
            || button == this.doubleBtn
            || button == this.beSubstitutionsEnabledBtn
            || button == this.beSubstitutionsDisabledBtn
            || button == this.combineEnableBtn
            || button == this.combineDisableBtn
            || button == this.tabProcessButton
            || button == this.tabCraftButton;
    }

    @Override
    public boolean hideItemPanelSlot(int tx, int ty, int tw, int th) {
        if (tw <= 0 || th <= 0) {
            return false;
        }
        for (PanelSection section : this.sections) {
            if (tx < this.absX + section.x + section.width && tx + tw > this.absX + section.x
                && ty < this.absY + section.y + section.height
                && ty + th > this.absY + section.y) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void mouseClicked(int xCoord, int yCoord, int btn) {
        if (this.container.isCraftingMode()) return;
        final int currentScroll = this.processingScrollBar.getCurrentScroll();
        this.processingScrollBar
            .click(this.parent, xCoord - this.parent.getGuiLeft(), yCoord - this.parent.getGuiTop());
        if (currentScroll != this.processingScrollBar.getCurrentScroll()) {
            changeActivePage();
        }
    }

    protected void changeActivePage() {
        AE2Thing.proxy.netHandler.sendToServer(
            new CPacketTerminalBtns("PatternTerminal.ActivePage", this.processingScrollBar.getCurrentScroll()));
    }

    @Override
    public boolean handleMouseClick(Slot slot, int slotIdx, int ctrlDown, int mouseButton) {
        if (!((slot instanceof SlotPatternFake) || (slot instanceof SlotFakeCraftingMatrix)
            || (slot instanceof SlotPatternTerm))) {
            return false;
        }
        if (mouseButton == 3 && !this.container.isCraftingMode()) {
            if (slot.getHasStack()) {
                IAEItemStack stack = AEItemStack.create(slot.getStack());
                this.inventorySlots.setTargetStack(stack);
                for (int i = 0; i < this.inventorySlots.inventorySlots.size(); i++) {
                    if (slot.equals(this.inventorySlots.inventorySlots.get(i))) {
                        if (isCtrlKeyDown() && !Util.isFluidPacket(stack.getItemStack())) {
                            InventoryActionExtend action = InventoryActionExtend.SET_PATTERN_NAME;
                            AE2Thing.proxy.netHandler
                                .sendToServer(new CPacketInventoryActionExtend(action, i, 0, stack));
                        } else {
                            InventoryActionExtend action = InventoryActionExtend.SET_PATTERN_VALUE;
                            AE2Thing.proxy.netHandler
                                .sendToServer(new CPacketInventoryActionExtend(action, i, 0, stack));
                        }
                    }
                }
                return true;
            }
        }

        InventoryAction action = ctrlDown == 1 ? InventoryAction.SPLIT_OR_PLACE_SINGLE
            : InventoryAction.PICKUP_OR_SET_DOWN;
        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) {
            if (action == InventoryAction.SPLIT_OR_PLACE_SINGLE) {
                action = InventoryAction.MOVE_REGION;
            } else {
                action = InventoryAction.SPLIT_OR_PLACE_SINGLE;
            }
        }
        if (Ae2ReflectClient.getDragClick(this.parent)
            .size() > 1) {
            return false;
        }
        final PacketInventoryAction p = new PacketInventoryAction(action, slotIdx, -1);
        NetworkHandler.instance.sendToServer(p);
        return true;
    }

    @Override
    public boolean actionPerformed(GuiButton btn) {
        if (this.tabCraftButton == btn || this.tabProcessButton == btn) {
            AE2Thing.proxy.netHandler.sendToServer(
                new CPacketTerminalBtns(
                    "PatternTerminal.CraftMode",
                    this.tabProcessButton == btn ? MODE_CRAFTING : MODE_PROCESSING));
            return true;
        } else if (this.encodeBtn == btn) {
            int value = (isCtrlKeyDown() ? 1 : 0) << 1 | (isShiftKeyDown() ? 1 : 0);
            AE2Thing.proxy.netHandler.sendToServer(new CPacketTerminalBtns("PatternTerminal.Encode", value));
            if (value == 0 && NEIClientUtils.altKey()) {
                MinecraftForge.EVENT_BUS.post(new EncodeEvent(true));
            }
            return true;
        } else if (this.clearBtn == btn) {
            AE2Thing.proxy.netHandler.sendToServer(new CPacketTerminalBtns("PatternTerminal.Clear", 1));
            return true;
        } else if (this.substitutionsEnabledBtn == btn || this.substitutionsDisabledBtn == btn) {
            AE2Thing.proxy.netHandler.sendToServer(
                new CPacketTerminalBtns("PatternTerminal.Substitute", this.substitutionsEnabledBtn == btn ? 0 : 1));
            return true;
        } else if (this.fluidPrioritizedEnabledBtn == btn || this.fluidPrioritizedDisabledBtn == btn) {
            AE2Thing.proxy.netHandler.sendToServer(
                new CPacketTerminalBtns(
                    "PatternTerminal.Prioritize",
                    isShiftKeyDown() ? 2 : (container.prioritize ? 0 : 1)));
            return true;
        } else if (this.invertBtn == btn) {
            AE2Thing.proxy.netHandler
                .sendToServer(new CPacketTerminalBtns("PatternTerminal.Invert", container.inverted ? 0 : 1));
            return true;
        } else if (this.combineDisableBtn == btn || this.combineEnableBtn == btn) {
            AE2Thing.proxy.netHandler.sendToServer(
                new CPacketTerminalBtns("PatternTerminal.Combine", this.combineDisableBtn == btn ? 1 : 0));
            return true;
        } else if (ModAndClassUtil.isDoubleButton && doubleBtn == btn) {
            final boolean backwards = Mouse.isButtonDown(1);
            int val = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ? 1 : 0;
            if (backwards) val |= 0b10;
            AE2Thing.proxy.netHandler.sendToServer(new CPacketTerminalBtns("PatternTerminal.Double", val));
            return true;
        } else if (ModAndClassUtil.isBeSubstitutionsButton && beSubstitutionsDisabledBtn == btn) {
            AE2Thing.proxy.netHandler.sendToServer(new CPacketTerminalBtns("PatternTerminal.beSubstitute", 1));
            return true;
        } else if (ModAndClassUtil.isBeSubstitutionsButton && beSubstitutionsEnabledBtn == btn) {
            AE2Thing.proxy.netHandler.sendToServer(new CPacketTerminalBtns("PatternTerminal.beSubstitute", 0));
            return true;
        }
        return false;
    }

    @Override
    public void mouseClickMove(int x, int y, int c, long d) {
        final Slot slot = this.gui.getSlot(x, y);
        final ItemStack itemstack = this.parent.mc.thePlayer.inventory.getItemStack();

        if (slot instanceof SlotFake && itemstack != null) {
            this.drag_click.add(slot);
            if (this.drag_click.size() > 1) {
                for (final Slot dr : this.drag_click) {
                    final PacketInventoryAction p = new PacketInventoryAction(
                        c == 0 ? InventoryAction.PICKUP_OR_SET_DOWN : InventoryAction.PLACE_SINGLE,
                        dr.slotNumber,
                        -1);
                    NetworkHandler.instance.sendToServer(p);
                }
            }
        }
        if (this.container.isCraftingMode()) return;
        final int currentScroll = this.processingScrollBar.getCurrentScroll();
        this.processingScrollBar.click(this.parent, x - this.parent.getGuiLeft(), y - this.parent.getGuiTop());
        if (currentScroll != this.processingScrollBar.getCurrentScroll()) {
            changeActivePage();
        }
    }

    @Override
    public boolean mouseWheelEvent(int mouseX, int mouseY, int wheel) {
        if (this.container.isCraftingMode()) return false;
        if (this.processingScrollBar.contains(mouseX - this.parent.getGuiLeft(), mouseY - this.parent.getGuiTop())) {
            final int currentScroll = this.processingScrollBar.getCurrentScroll();
            this.processingScrollBar.wheel(wheel);
            if (currentScroll != this.processingScrollBar.getCurrentScroll()) {
                changeActivePage();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyTyped(char character, int key) {
        return false;
    }

    @Override
    public boolean draggable() {
        return true;
    }

    @Override
    public Rectangle getRectangle() {
        return new Rectangle(this.absX, this.absY, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public void setRectangle(int x, int y) {
        this.absX = x;
        this.absY = y;
        this.updateScrollBarPosition();
        this.updateFixedButtonPositions();
        this.updateSlotPositions();
    }

    private void updateScrollBarPosition() {
        this.processingScrollBar.setTop(this.absY - this.parent.getGuiTop() + 9)
            .setLeft(this.absX - this.parent.getGuiLeft() + 6);
    }

    public void updateSlotPositions() {
        int panelX = this.absX - this.parent.getGuiLeft();
        int panelY = this.absY - this.parent.getGuiTop();
        for (SlotLayout layout : this.slotLayouts) {
            AppEngSlot slot = layout.slot;
            slot.yDisplayPosition = panelY + layout.y;
            if (!this.container.isCraftingMode()
                && (slot instanceof SlotFakeCraftingMatrix || slot instanceof SlotPatternTerm)) {
                slot.xDisplayPosition = -9000;
                continue;
            }
            int hiddenOffset = slot instanceof SlotPatternFake fake ? fake.getDisplayPositionOffset() : 0;
            slot.xDisplayPosition = panelX + layout.x + hiddenOffset;
        }
    }

    private void captureSlotLayout() {
        this.slotLayouts.clear();
        for (Object object : this.container.inventorySlots) {
            if (!(object instanceof AppEngSlot slot) || !this.ownsSlot(slot)) continue;
            int localX = slot.getX() - PANEL_X;
            int localY = slot.yDisplayPosition - PANEL_Y;
            this.slotLayouts.add(new SlotLayout(slot, localX, localY));
        }
    }

    private boolean ownsSlot(Slot slot) {
        return slot instanceof SlotPattern || slot instanceof SlotPatternFake
            || slot instanceof SlotFakeCraftingMatrix
            || slot instanceof SlotPatternTerm
            || slot instanceof SlotRestrictedInput;
    }

    public boolean isInteractiveAt(int x, int y) {
        return !this.container.isCraftingMode()
            && this.processingScrollBar.contains(x - this.parent.getGuiLeft(), y - this.parent.getGuiTop());
    }

    private record PanelSection(int x, int y, int width, int height, int textureX, int textureY) {

    }

    private record SlotLayout(AppEngSlot slot, int x, int y) {

    }
}
