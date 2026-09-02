package com.asdflj.ae2thing.client.gui.widget;

import static net.minecraft.client.gui.GuiScreen.isShiftKeyDown;
import static appeng.client.gui.AEBaseGui.isCtrlKeyDown;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.client.gui.BaseMEGui;
import com.asdflj.ae2thing.client.gui.IGuiMonitorTerminal;
import com.asdflj.ae2thing.client.gui.IWidgetGui;
import com.asdflj.ae2thing.client.gui.container.ContainerWirelessDualInterfaceTerminal;
import com.asdflj.ae2thing.client.me.AdvItemRepo;
import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.network.CPacketInventoryAction;
import com.asdflj.ae2thing.util.Ae2ReflectClient;
import com.asdflj.ae2thing.util.AspectUtil;
import com.glodblock.github.common.item.ItemFluidDrop;

import appeng.api.config.SearchBoxMode;
import appeng.api.config.Settings;
import appeng.api.config.TerminalStyle;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.AEStackTypeRegistry;
import appeng.api.storage.data.IAEStackType;
import appeng.api.util.IConfigManager;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.slots.VirtualMEMonitorableSlot;
import appeng.client.gui.slots.VirtualMESlot;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.IDropToFillTextField;
import appeng.client.gui.widgets.ISortSource;
import appeng.container.AEBaseContainer;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketMonitorableAction;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.InventoryAction;
import appeng.helpers.MonitorableAction;
import appeng.util.IConfigManagerHost;
import appeng.util.Platform;
import codechicken.nei.LayoutManager;
import codechicken.nei.util.TextHistory;

public class ItemPanel implements IAEBasePanel, IGuiMonitorTerminal, IConfigManagerHost, IDropToFillTextField {

    private final BaseMEGui parent;
    private final IWidgetGui gui;
    private final ContainerWirelessDualInterfaceTerminal container;
    private final int perRow;
    private final int rows;
    protected THGuiTextField searchField;
    private final AEBaseContainer inventorySlots;
    private final AdvItemRepo repo;
    private final IConfigManager configSrc;
    private final GuiScrollbar scrollbar;
    private int absX;
    private int absY;
    private final int w;
    private final int h;
    private int offsetY;
    private final boolean showViewBtn = true;
    private GuiImgButton SortByBox;
    private GuiImgButton ViewBox;
    private GuiImgButton SortDirBox;
    private GuiImgButton searchBoxSettings;
    private static String memoryText = "";
    private final TextHistory history;

    public ItemPanel(IWidgetGui gui, ContainerWirelessDualInterfaceTerminal container, IConfigManager configSrc,
        ISortSource source) {
        this.gui = gui;
        this.container = container;
        this.parent = gui.getGui();
        this.inventorySlots = this.container;
        this.configSrc = configSrc;
        this.scrollbar = new GuiScrollbar();
        this.repo = new AdvItemRepo(scrollbar, source);
        this.repo.setCache(this);
        this.repo.setPowered(true);
        this.w = 101;
        this.h = 96;
        this.repo.setRowSize(4);
        this.rows = 4;
        this.perRow = 4;
        this.history = Ae2ReflectClient.getHistory(LayoutManager.searchField);
    }

    public void saveSearchString() {
        if (Mods.NOT_ENOUGH_ITEMS.isModLoaded() && isNEISearch()
            && !this.searchField.getText()
                .isEmpty()) {
            this.history.add(this.searchField.getText());
        }
    }

    protected boolean isNEISearch() {
        final Enum<?> s = AEConfig.instance.settings.getSetting(Settings.SEARCH_MODE);
        return s == SearchBoxMode.NEI_MANUAL_SEARCH || s == SearchBoxMode.NEI_AUTOSEARCH;
    }

    @Override
    public String getBackground() {
        return "gui/widget/items.png";
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        GL11.glTranslatef(0f, 0f, 100f);
        this.scrollbar.draw(this.parent);
        GL11.glTranslatef(0f, 0f, -100f);
        if (AEConfig.instance.preserveSearchBar && searchField != null && searchField.isMouseIn(mouseX, mouseY))
            this.parent.drawTooltip(this.absX - offsetX, this.absY - 20, 0, searchField.getMessage());
        if (Mods.NOT_ENOUGH_ITEMS.isModLoaded() && searchField != null
            && this.searchField.isMouseIn(mouseX, mouseY)
            && this.isNEISearch()
            && this.parent != null) {
            // draw selection
            List<String> list = Ae2ReflectClient.getHistoryList(this.history);
            this.parent.drawHistorySelection(
                searchField.xPosition - offsetX,
                searchField.yPosition - this.parent.getGuiTop(),
                searchField.getText(),
                searchField.width,
                list);
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTextureBack(getBackground());
        this.parent.drawTexturedModalRect(absX, absY, 0, 0, 101, 96);
        if (this.searchField != null) {
            this.searchField.drawTextBox();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float btn) {}

    @Override
    public void initGui() {
        this.absX = this.parent.getGuiLeft() - 101;
        this.absY = this.parent.getGuiTop() + this.parent.getYSize() - 96;
        this.searchField = new THGuiTextField(this.parent.getFontRenderer(), absX + 3, absY + 4, 72, 12);
        this.searchField.setEnableBackgroundDrawing(false);
        this.searchField.setMaxStringLength(25);
        this.searchField.setTextColor(0xFFFFFF);
        this.searchField.setVisible(true);
        // this.searchField.setMessage(ButtonToolTips.SearchStringTooltip.getLocal());
        this.gui.getMeSlots()
            .clear();
        for (int y = 0; y < this.rows; y++) {
            for (int x = 0; x < this.perRow; x++) {
                this.gui.registerMESlot(
                    new VirtualMEMonitorableSlot(
                        (this.absX - this.parent.getGuiLeft() + 5) + x * 18,
                        (this.absY + 18 - this.parent.getGuiTop()) + y * 18,
                        this.repo,
                        x + y * this.perRow,
                        type -> true));
            }
        }
        this.offsetY = this.absY;
        this.gui.getButtonList()
            .add(
                this.SortByBox = new GuiImgButton(
                    this.absX - 18,
                    this.offsetY,
                    Settings.SORT_BY,
                    this.configSrc.getSetting(Settings.SORT_BY)));
        this.offsetY += 20;

        if (this.showViewBtn) {
            this.gui.getButtonList()
                .add(
                    this.ViewBox = new GuiImgButton(
                        this.absX - 18,
                        this.offsetY,
                        Settings.VIEW_MODE,
                        this.configSrc.getSetting(Settings.VIEW_MODE)));
            this.offsetY += 20;
        }

        this.gui.getButtonList()
            .add(
                this.SortDirBox = new GuiImgButton(
                    this.absX - 18,
                    this.offsetY,
                    Settings.SORT_DIRECTION,
                    this.configSrc.getSetting(Settings.SORT_DIRECTION)));
        this.offsetY += 20;

        this.gui.getButtonList()
            .add(
                this.searchBoxSettings = new GuiImgButton(
                    this.absX - 18,
                    this.offsetY,
                    Settings.SEARCH_MODE,
                    AEConfig.instance.settings.getSetting(Settings.SEARCH_MODE)));
        this.offsetY += 20;
        final Enum<?> searchMode = AEConfig.instance.settings.getSetting(Settings.SEARCH_MODE);

        if (searchMode == SearchBoxMode.AUTOSEARCH || searchMode == SearchBoxMode.NEI_AUTOSEARCH) {
            this.searchField.setFocused(true);
        }
        if ((AEConfig.instance.preserveSearchBar || this.parent.isSubGui())) {
            setSearchString(memoryText, false);
        }
        if (this.parent.isSubGui()) {
            this.repo.updateView();
        }
        this.setScrollBar();
    }

    @Override
    public boolean hideItemPanelSlot(int tx, int ty, int tw, int th) {
        int rw = 101;
        int rh = 96;
        if (tw <= 0 || th <= 0) {
            return false;
        }

        int rx = this.absX;
        int ry = this.absY;

        rw += rx;
        rh += ry;
        tw += tx;
        th += ty;

        // overflow || intersect
        return (rw < rx || rw > tx) && (rh < ry || rh > ty) && (tw < tx || tw > rx) && (th < ty || th > ry);
    }

    @Override
    public void mouseClicked(int xCoord, int yCoord, int btn) {
        this.saveSearchString();
        this.searchField.mouseClicked(xCoord, yCoord, btn);
        if (btn == 1 && this.searchField.isMouseIn(xCoord, yCoord)) {
            setSearchString("", true);
        }
        this.scrollbar.click(this.parent, xCoord - this.parent.getGuiLeft(), yCoord - this.parent.getGuiTop());
    }

    public void setSearchString(String memoryText, boolean updateView) {
        this.searchField.setText(memoryText);
        this.repo.setSearchString(memoryText);
        if (updateView) {
            this.repo.updateView();
            this.setScrollBar();
        }
        updateSuggestion();
    }

    @Override
    public void setScrollBar() {
        this.scrollbar.setTop(this.absY - this.parent.getGuiTop() + 18)
            .setLeft(this.absX - this.parent.getGuiLeft() + this.w - 20)
            .setHeight(this.rows * 18 - 2);
        this.scrollbar
            .setRange(0, (this.repo.size() + this.perRow - 1) / this.perRow - this.rows, Math.max(1, this.rows / 6));
    }

    @Override
    public boolean isOverTextField(int mousex, int mousey) {
        return searchField.isMouseIn(mousex, mousey);
    }

    @Override
    public void setTextFieldValue(String displayName, int mousex, int mousey, ItemStack stack) {
        if (!searchField.isMouseIn(mousex, mousey)) return;
        if (Mods.THAUMIC_ENERGISTICS.isModLoaded() && AspectUtil.getAspectFromJar(stack) != null) {
            setSearchString(
                Objects.requireNonNull(AspectUtil.getAspectFromJar(stack))
                    .getName(),
                true);
        } else {
            setSearchString(displayName, true);
        }
        this.saveSearchString();
    }

    private boolean meSlotClick(VirtualMESlot slot, int ctrlDown, int clickMode) {
        saveSearchString();
        final EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        IAEStack<?> aeStack = slot.getAEStack();
        IAEItemStack itemStack = aeStack instanceof IAEItemStack ais ? ais : null;

        if (isCtrlKeyDown()) {
            final ItemStack hand = player.inventory.getItemStack();
            if (hand != null) {
                for (final IAEStackType<?> type : AEStackTypeRegistry.getAllTypes()) {
                    if (!type.isContainerItemForType(hand)) continue;
                    if (ctrlDown == 0 && aeStack == null) continue;
                    final boolean processStack = isShiftKeyDown();
                    final MonitorableAction action;
                    if (ctrlDown == 0) {
                        action = processStack
                            ? MonitorableAction.FILL_CONTAINERS
                            : MonitorableAction.FILL_SINGLE_CONTAINER;
                    } else {
                        action = processStack
                            ? MonitorableAction.DRAIN_CONTAINERS
                            : MonitorableAction.DRAIN_SINGLE_CONTAINER;
                    }
                    this.inventorySlots.setTargetStack(aeStack);
                    NetworkHandler.instance.sendToServer(new PacketMonitorableAction(action, -1));
                    return true;
                }
            }
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
            this.inventorySlots.setTargetStack(itemStack);
            final PacketMonitorableAction p = new PacketMonitorableAction(MonitorableAction.MOVE_REGION, -1);
            NetworkHandler.instance.sendToServer(p);
            return true;
        }

        MonitorableAction action = null;
        switch (clickMode) {
            case 0: // pickup / set-down.
                action = ctrlDown == 1 ? MonitorableAction.SPLIT_OR_PLACE_SINGLE : MonitorableAction.PICKUP_OR_SET_DOWN;
                if (aeStack != null && action == MonitorableAction.PICKUP_OR_SET_DOWN
                    && aeStack.getStackSize() == 0
                    && player.inventory.getItemStack() == null) {
                    action = MonitorableAction.AUTO_CRAFT;
                }
                break;
            case 1:
                action = ctrlDown == 1 ? MonitorableAction.PICKUP_SINGLE : MonitorableAction.SHIFT_CLICK;
                break;
            case 3: // creative dupe:
                if (aeStack != null && aeStack.isCraftable()) {
                    action = MonitorableAction.AUTO_CRAFT;
                } else if (player.capabilities.isCreativeMode) {
                    if (itemStack != null) {
                        action = MonitorableAction.CREATIVE_DUPLICATE;
                    }
                } else break;
            default:
        }
        if (action == MonitorableAction.AUTO_CRAFT) {
            this.inventorySlots.setTargetStack(aeStack);
            AE2Thing.proxy.netHandler.sendToServer(
                new CPacketInventoryAction(
                    InventoryAction.AUTO_CRAFT,
                    Ae2ReflectClient.getInventorySlots(this.parent)
                        .size(),
                    -2,
                    aeStack));
        } else if (action != null) {
            if (itemStack != null && itemStack.getItem() instanceof ItemFluidDrop) itemStack = null;
            this.inventorySlots.setTargetStack(itemStack);
            final PacketMonitorableAction p = new PacketMonitorableAction(action, -1);
            NetworkHandler.instance.sendToServer(p);
        }
        return true;
    }

    @Override
    public boolean handleVirtualSlotClick(VirtualMESlot slot, int mouseButton) {
        if (!(slot instanceof VirtualMEMonitorableSlot)) return false;
        final boolean pickBlock = mouseButton == GuiMEMonitorable.keyBindPickBlockAction;
        final int ctrlDown = pickBlock ? 0 : mouseButton;
        final int clickMode = pickBlock ? 3 : (isShiftKeyDown() ? 1 : 0);
        return meSlotClick(slot, ctrlDown, clickMode);
    }

    @Override
    public boolean handleMouseClick(Slot slot, int slotIdx, int ctrlDown, int mouseButton) {
        return false;
    }

    @Override
    public boolean actionPerformed(GuiButton btn) {
        if (btn instanceof final GuiImgButton iBtn) {
            final boolean backwards = Mouse.isButtonDown(1);
            if (iBtn.getSetting() != Settings.ACTIONS) {
                final Enum<?> cv = iBtn.getCurrentValue();
                final Enum<?> next = Platform.rotateEnum(cv, backwards, iBtn.getSetting().getPossibleValues());
                if (btn == this.searchBoxSettings) {
                    AEConfig.instance.settings.putSetting(iBtn.getSetting(), next);
                } else if (btn == this.SortByBox || btn == this.SortDirBox || btn == this.ViewBox) {
                    try {
                        NetworkHandler.instance
                            .sendToServer(new PacketValueConfig(iBtn.getSetting().name(), next.name()));
                    } catch (final IOException e) {
                        AELog.debug(e);
                    }
                    iBtn.set(next);
                    if (next.getClass() == SearchBoxMode.class || next.getClass() == TerminalStyle.class) {
                        this.reInitalize();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private void reInitalize() {
        this.gui.getButtonList()
            .clear();
        this.initGui();
    }

    @Override
    public void mouseClickMove(int x, int y, int c, long d) {
        this.scrollbar.click(this.parent, x - this.parent.getGuiLeft(), y - this.parent.getGuiTop());
    }

    @Override
    public boolean mouseWheelEvent(int mouseX, int mouseY, int wheel) {
        if (Mods.NOT_ENOUGH_ITEMS.isModLoaded() && this.searchField.isMouseIn(mouseX, mouseY) && isNEISearch()) {
            TextHistory.Direction direction;
            switch (wheel) {
                case -1:
                    direction = TextHistory.Direction.PREVIOUS;
                    break;
                case 1:
                    direction = TextHistory.Direction.NEXT;
                    break;
                default:
                    return false;
            }
            this.history.get(direction, this.searchField.getText())
                .ifPresent(t -> setSearchString(t, true));
            return true;
        }
        if (this.scrollbar.contains(mouseX - this.parent.getGuiLeft(), mouseY - this.parent.getGuiTop())
            || (mouseX > this.absX && mouseX < this.absX + this.w
                && mouseY > this.absY + 18
                && mouseY < this.absY + this.h)) {
            this.saveSearchString();
            final int currentScroll = this.scrollbar.getCurrentScroll();
            this.scrollbar.wheel(wheel);
            return currentScroll != this.scrollbar.getCurrentScroll();
        }
        return mouseX > this.absX && mouseX < this.absX + this.w && mouseY > this.absY && mouseY < this.absY + this.h;
    }

    private void updateSuggestion() {
        if (Mods.NOT_ENOUGH_ITEMS.isModLoaded() && this.isNEISearch()) {
            if (this.searchField.getText()
                .isEmpty()) {
                this.setSuggestion("");
                return;
            }
            String history = this.findHistoryPrefix(this.searchField.getText());
            if (history != null) {
                this.setSuggestion(history);
            } else {
                this.setSuggestion("");
            }
        }
    }

    private String findHistoryPrefix(String prefix) {
        for (String value : Ae2ReflectClient.getHistoryList(this.history)) {
            if (value.startsWith(prefix)) {
                return value;
            }
        }
        return null;
    }

    private void setSuggestion(String suggestion) {
        this.searchField.setSuggestion(suggestion);
    }

    @Override
    public boolean keyTyped(char character, int key) {
        if (Mods.NOT_ENOUGH_ITEMS.isModLoaded() && this.isNEISearch()) {
            if (key == Keyboard.KEY_TAB && this.searchField.isFocused()) {
                String history = this.findHistoryPrefix(this.searchField.getText());
                if (history != null) {
                    setSearchString(history, true);
                }
                return true;
            } else if (key == Keyboard.KEY_DELETE) {
                String next = this.history.getNext(this.searchField.getText())
                    .orElse("");
                Ae2ReflectClient.getHistoryList(this.history)
                    .removeIf(s -> s.equals(this.searchField.getText()));
                setSearchString(next, true);
                return true;
            }
        }
        if (this.searchField.isFocused()) {
            if (character == ' ' && this.searchField.getText()
                .isEmpty()) {
                return false;
            }

            if (this.searchField.textboxKeyTyped(character, key)) {
                this.repo.setSearchString(this.searchField.getText());
                this.repo.updateView();
                this.setScrollBar();
                this.updateSuggestion();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean draggable() {
        return false;
    }

    @Override
    public Rectangle getRectangle() {
        return new Rectangle(this.absX, this.absY, w, h);
    }

    @Override
    public void setRectangle(int x, int y) {
        this.absX = x;
        this.absY = y;
    }

    @Override
    public void postStackUpdate(List<? extends IAEStack<?>> list) {
        for (IAEStack<?> stack : list) {
            if (stack instanceof IAEItemStack item && item.getItem() instanceof ItemFluidDrop) continue;
            this.repo.postUpdate(stack);
        }
        this.repo.updateView();
        if (!this.repo.hasCache()) {
            this.setScrollBar();
        }
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
    public void onGuiClosed() {
        memoryText = this.searchField.getText();
    }

    @Override
    public void updateSetting(IConfigManager manager, Enum settingName, Enum newValue) {
        if (this.SortByBox != null) {
            this.SortByBox.set(this.configSrc.getSetting(Settings.SORT_BY));
        }
        if (this.SortDirBox != null) {
            this.SortDirBox.set(this.configSrc.getSetting(Settings.SORT_DIRECTION));
        }
        if (this.ViewBox != null) {
            this.ViewBox.set(this.configSrc.getSetting(Settings.VIEW_MODE));
        }

        this.repo.updateView();
    }

    public AdvItemRepo getRepo() {
        return repo;
    }

    @Override
    public void handleKeyboardInput() {
        this.getRepo()
            .setPaused(this.parent.hasShiftDown());
    }

    @Override
    public void setPlayerInv(ItemStack is) {
        this.container.getPlayerInv()
            .setItemStack(is);
    }

    @Override
    public THGuiTextField getSearchField() {
        return this.searchField;
    }
}
