package com.asdflj.ae2thing.client.gui;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.client.gui.container.ContainerMonitor;
import com.asdflj.ae2thing.client.gui.widget.ITypeFilterGui;
import com.asdflj.ae2thing.client.gui.widget.THGuiTextField;
import com.asdflj.ae2thing.client.gui.widget.TypeFilterWidget;
import com.asdflj.ae2thing.client.me.AdvItemRepo;
import com.asdflj.ae2thing.integration.Mods;
import com.asdflj.ae2thing.inventory.InventoryHandler;
import com.asdflj.ae2thing.inventory.gui.GuiType;
import com.asdflj.ae2thing.network.CPacketInventoryAction;
import com.asdflj.ae2thing.util.Ae2ReflectClient;
import com.asdflj.ae2thing.util.AspectUtil;
import com.asdflj.ae2thing.util.ModAndClassUtil;
import com.glodblock.github.common.item.ItemFluidDrop;

import appeng.api.config.CraftingStatus;
import appeng.api.config.SearchBoxMode;
import appeng.api.config.Settings;
import appeng.api.config.TerminalStyle;
import appeng.api.config.YesNo;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.util.IConfigManager;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.slots.VirtualMEMonitorableSlot;
import appeng.client.gui.slots.VirtualMESlot;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.gui.widgets.IDropToFillTextField;
import appeng.client.gui.widgets.ISortSource;
import appeng.container.AEBaseContainer;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.SlotCraftingMatrix;
import appeng.container.slot.SlotCraftingTerm;
import appeng.container.slot.SlotDisabled;
import appeng.container.slot.SlotFake;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.container.slot.SlotPatternTerm;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.core.sync.packets.PacketMonitorableAction;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.InventoryAction;
import appeng.helpers.MonitorableAction;
import appeng.integration.IntegrationRegistry;
import appeng.integration.IntegrationType;
import appeng.integration.modules.NEI;
import appeng.util.IConfigManagerHost;
import appeng.util.MonitorableTypeFilter;
import appeng.util.Platform;
import codechicken.nei.util.TextHistory;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;

public abstract class GuiMonitor extends BaseMEGui implements IConfigManagerHost, ISortSource, IDropToFillTextField,
    IGuiDrawSlot, IGuiMonitorTerminal, ITypeFilterGui {

    protected GuiImgButton clearBtn;
    public static int craftingGridOffsetX;
    public static int craftingGridOffsetY;
    protected static String memoryText = "";
    protected final int offsetX = 9;
    protected final int lowerTextureOffset = 0;
    protected AdvItemRepo repo;
    protected THGuiTextField searchField;
    protected int perRow = 9;
    protected int reservedSpace = 0;
    protected int rows = 0;
    protected int maxRows = Integer.MAX_VALUE;
    protected int standardSize;
    protected int offsetY;
    protected GuiTabButton craftingStatusBtn;
    protected GuiImgButton craftingStatusImgBtn;
    protected GuiImgButton SortByBox;
    protected GuiImgButton SortDirBox;
    protected GuiImgButton searchBoxSettings;
    protected GuiImgButton terminalStyleBox;
    protected GuiImgButton searchStringSave;
    protected GuiImgButton ViewBox;
    protected boolean showViewBtn = true;
    protected boolean viewCell = true;
    protected final ContainerMonitor container;
    protected final TypeFilterWidget typeFilter;

    public GuiMonitor(Container container) {
        super(container);
        final GuiScrollbar scrollbar = new GuiScrollbar();
        this.setScrollBar(scrollbar);
        this.container = (ContainerMonitor) container;
        this.typeFilter = new TypeFilterWidget(container.windowId);
        this.typeFilter.setFilters(MonitorableTypeFilter.createDefaultMap());
        this.repo = new AdvItemRepo(getScrollBar(), this);
        this.repo.setPowered(true);
    }

    protected void saveSearchString() {
        if (Mods.NOT_ENOUGH_ITEMS.isModLoaded() && isNEISearch()
            && !this.searchField.getText()
                .isEmpty()) {
            this.history.add(this.searchField.getText());
        }
    }

    @Override
    protected void handleMouseClick(final Slot slot, final int slotIdx, final int ctrlDown, final int mouseButton) {
        saveSearchString();

        if (slot instanceof SlotFake) {
            InventoryAction action = ctrlDown == 1 ? InventoryAction.SPLIT_OR_PLACE_SINGLE
                : InventoryAction.PICKUP_OR_SET_DOWN;
            if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) {
                if (action == InventoryAction.SPLIT_OR_PLACE_SINGLE) {
                    action = InventoryAction.MOVE_REGION;
                } else {
                    action = InventoryAction.SPLIT_OR_PLACE_SINGLE;
                }
            }
            if (Ae2ReflectClient.getDragClick(this)
                .size() > 1) {
                return;
            }
            final PacketInventoryAction p = new PacketInventoryAction(action, slotIdx, 0);
            NetworkHandler.instance.sendToServer(p);
            return;
        }

        if (slot instanceof SlotPatternTerm) {
            if (mouseButton == 6) {
                return; // prevents weird double clicks
            }
            try {
                NetworkHandler.instance.sendToServer(((SlotPatternTerm) slot).getRequest(isShiftKeyDown()));
            } catch (final IOException e) {
                AELog.debug(e);
            }
        } else if (slot instanceof SlotCraftingTerm) {
            if (mouseButton == 6) {
                return; // prevents weird double clicks
            }
            InventoryAction action;
            if (isShiftKeyDown()) {
                action = InventoryAction.CRAFT_SHIFT;
            } else {
                // Craft a stack on right-click, craft a single one on left-click
                action = (mouseButton == 1) ? InventoryAction.CRAFT_STACK : InventoryAction.CRAFT_ITEM;
            }
            final PacketInventoryAction p = new PacketInventoryAction(action, slotIdx, 0);
            NetworkHandler.instance.sendToServer(p);
            return;
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
            if (this.enableSpaceClicking() && !(slot instanceof SlotPatternTerm)) {
                int slotNum = Ae2ReflectClient.getInventorySlots(this)
                    .size();
                if (slot != null) {
                    slotNum = slot.slotNumber;
                }
                ((AEBaseContainer) this.inventorySlots).setTargetStack(null);
                final PacketInventoryAction p = new PacketInventoryAction(InventoryAction.MOVE_REGION, slotNum, 0);
                NetworkHandler.instance.sendToServer(p);
                return;
            }
        }

        super.handleMouseClick(slot, slotIdx, ctrlDown, mouseButton);
    }

    @Override
    protected boolean handleVirtualSlotClick(final VirtualMESlot virtualSlot, final int mouseButton) {
        if (!(virtualSlot instanceof VirtualMEMonitorableSlot)) {
            return super.handleVirtualSlotClick(virtualSlot, mouseButton);
        }
        saveSearchString();
        final EntityPlayer player = Minecraft.getMinecraft().thePlayer;

        // mouseButton is the raw mouse button (0 left, 1 right) or keyBindPickBlockAction for pick-block.
        // Reconstruct the legacy parameters: ctrlDown = mouse button index, clickMode = 0 normal / 1 shift / 3 pick.
        final boolean pickBlock = mouseButton == GuiMEMonitorable.keyBindPickBlockAction;
        final int ctrlDown = pickBlock ? 0 : mouseButton;
        final int clickMode = pickBlock ? 3 : (isShiftKeyDown() ? 1 : 0);

        if (updateFluidContainer(virtualSlot, ctrlDown, clickMode)) return true;

        if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
            if (this.enableSpaceClicking()) {
                IAEStack<?> aeStack = virtualSlot.getAEStack();
                IAEItemStack stack = aeStack instanceof IAEItemStack ais ? ais : null;
                ((AEBaseContainer) this.inventorySlots).setTargetStack(stack);
                final PacketMonitorableAction p = new PacketMonitorableAction(MonitorableAction.MOVE_REGION, -1);
                NetworkHandler.instance.sendToServer(p);
                return true;
            }
        }

        MonitorableAction action = null;
        IAEStack<?> aeStack = virtualSlot.getAEStack();
        IAEItemStack stack = aeStack instanceof IAEItemStack ais ? ais : null;
        switch (clickMode) {
            case 0: // pickup / set-down.
                action = ctrlDown == 1 ? MonitorableAction.SPLIT_OR_PLACE_SINGLE : MonitorableAction.PICKUP_OR_SET_DOWN;
                if (stack != null && action == MonitorableAction.PICKUP_OR_SET_DOWN
                    && stack.getStackSize() == 0
                    && player.inventory.getItemStack() == null) {
                    action = MonitorableAction.AUTO_CRAFT;
                }
                break;
            case 1:
                action = ctrlDown == 1 ? MonitorableAction.PICKUP_SINGLE : MonitorableAction.SHIFT_CLICK;
                break;
            case 3: // creative dupe:
                stack = transformItem(stack); // for fluid terminal
                if (stack != null && stack.isCraftable()) {
                    action = MonitorableAction.AUTO_CRAFT;
                } else if (player.capabilities.isCreativeMode) {
                    if (aeStack instanceof IAEItemStack) {
                        action = MonitorableAction.CREATIVE_DUPLICATE;
                    }
                } else break;
            default:
        }
        if (action == MonitorableAction.AUTO_CRAFT) {
            ((AEBaseContainer) this.inventorySlots).setTargetStack(stack);
            AE2Thing.proxy.netHandler.sendToServer(
                new CPacketInventoryAction(
                    InventoryAction.AUTO_CRAFT,
                    Ae2ReflectClient.getInventorySlots(this)
                        .size(),
                    0,
                    stack));
        } else if (action != null) {
            if (stack != null && stack.getItem() instanceof ItemFluidDrop) stack = null;
            ((AEBaseContainer) this.inventorySlots).setTargetStack(stack);
            final PacketMonitorableAction p = new PacketMonitorableAction(action, -1);
            NetworkHandler.instance.sendToServer(p);
        }
        return true;
    }

    protected IAEItemStack transformItem(IAEItemStack stack) {
        return stack;
    }

    protected int getMaxRows() {
        return AEConfig.instance.getConfigManager()
            .getSetting(Settings.TERMINAL_STYLE) == TerminalStyle.SMALL ? 6 : Integer.MAX_VALUE;
    }

    public void setScrollBar() {
        this.getScrollBar()
            .setTop(18)
            .setLeft(175)
            .setHeight(this.rows * 18 - 2);
        this.getScrollBar()
            .setRange(0, (this.repo.size() + this.perRow - 1) / this.perRow - this.rows, Math.max(1, this.rows / 6));
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        this.maxRows = this.getMaxRows();
        this.perRow = AEConfig.instance.getConfigManager()
            .getSetting(Settings.TERMINAL_STYLE) != TerminalStyle.FULL ? 9
                : 9 + ((this.width - this.standardSize) / 18);

        final boolean hasNEI = IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.NEI);

        final int NEI = 0;
        int top = hasNEI ? 22 : 0;

        final int magicNumber = 114 + 1;
        final int extraSpace = this.height - magicNumber - NEI - top - this.reservedSpace;

        this.rows = (int) Math.floor(extraSpace / 18.0);
        if (this.rows > this.maxRows) {
            this.rows = this.maxRows;
        }

        if (hasNEI) {
            this.rows--;
        }

        if (this.rows < 3) {
            this.rows = 3;
        }

        if (AEConfig.instance.getConfigManager()
            .getSetting(Settings.TERMINAL_STYLE) != TerminalStyle.FULL) {
            this.xSize = this.standardSize + ((this.perRow - 9) * 18);
        } else {
            this.xSize = this.standardSize;
        }

        super.initGui();
        // full size : 204
        // extra slots : 72
        // slot 18

        // super.initGui() clears the registered virtual slots, so build the ME grid afterwards.
        this.getMeSlots()
            .clear();
        for (int y = 0; y < this.rows; y++) {
            for (int x = 0; x < this.perRow; x++) {
                this.registerMESlot(
                    new VirtualMEMonitorableSlot(
                        this.offsetX + x * 18,
                        18 + y * 18,
                        this.repo,
                        x + y * this.perRow,
                        type -> true));
            }
        }

        this.ySize = magicNumber + this.rows * 18 + this.reservedSpace;
        final int unusedSpace = this.height - this.ySize;
        this.guiTop = (int) Math.floor(unusedSpace / (unusedSpace < 0 ? 3.8f : 2.0f));

        this.offsetY = this.guiTop + 8;

        this.buttonList.add(
            this.SortByBox = new GuiImgButton(
                this.guiLeft - 18,
                this.offsetY,
                Settings.SORT_BY,
                this.configSrc.getSetting(Settings.SORT_BY)));
        this.offsetY += 20;

        if (this.showViewBtn) {
            this.buttonList.add(
                this.ViewBox = new GuiImgButton(
                    this.guiLeft - 18,
                    this.offsetY,
                    Settings.VIEW_MODE,
                    this.configSrc.getSetting(Settings.VIEW_MODE)));
            this.offsetY += 20;
        }

        this.buttonList.add(
            this.SortDirBox = new GuiImgButton(
                this.guiLeft - 18,
                this.offsetY,
                Settings.SORT_DIRECTION,
                this.configSrc.getSetting(Settings.SORT_DIRECTION)));
        this.offsetY += 20;

        this.buttonList.add(
            this.searchBoxSettings = new GuiImgButton(
                this.guiLeft - 18,
                this.offsetY,
                Settings.SEARCH_MODE,
                AEConfig.instance.settings.getSetting(Settings.SEARCH_MODE)));
        this.offsetY += 20;

        this.buttonList.add(
            this.searchStringSave = new GuiImgButton(
                this.guiLeft - 18,
                this.offsetY,
                Settings.SAVE_SEARCH,
                AEConfig.instance.preserveSearchBar ? YesNo.YES : YesNo.NO));
        this.offsetY += 20;

        this.buttonList.add(
            this.terminalStyleBox = new GuiImgButton(
                this.guiLeft - 18,
                this.offsetY,
                Settings.TERMINAL_STYLE,
                AEConfig.instance.settings.getSetting(Settings.TERMINAL_STYLE)));
        this.offsetY += 20;

        this.typeFilter.init(this.buttonList, this.guiLeft - 36, this.guiTop + 8);

        // Right now 80 > offsetX, but that can be changed later.
        // noinspection DataFlowIssue
        this.searchField = new THGuiTextField(
            this.fontRendererObj,
            this.guiLeft + Math.max(80, this.offsetX),
            this.guiTop + 4,
            90,
            12);
        this.searchField.setMessage(ButtonToolTips.SearchStringTooltip.getLocal());
        if (this.viewCell) {
            if (ModAndClassUtil.isCraftStatus && AEConfig.instance.getConfigManager()
                .getSetting(Settings.CRAFTING_STATUS)
                .equals(CraftingStatus.BUTTON)) {
                this.buttonList.add(
                    this.craftingStatusImgBtn = new GuiImgButton(
                        this.guiLeft - 18,
                        this.offsetY,
                        Settings.CRAFTING_STATUS,
                        AEConfig.instance.settings.getSetting(Settings.CRAFTING_STATUS)));
                this.offsetY += 20;
            } else {
                this.buttonList.add(
                    this.craftingStatusBtn = new GuiTabButton(
                        this.guiLeft + 170,
                        this.guiTop - 4,
                        2 + 11 * 16,
                        GuiText.CraftingStatus.getLocal(),
                        itemRender));
                this.craftingStatusBtn.setHideEdge(13); // GuiTabButton implementation //
            }
        }

        final Enum<?> setting = AEConfig.instance.settings.getSetting(Settings.SEARCH_MODE);
        this.searchField.setFocused(SearchBoxMode.AUTOSEARCH == setting || SearchBoxMode.NEI_AUTOSEARCH == setting);

        if ((AEConfig.instance.preserveSearchBar || this.isSubGui())) {
            setSearchString(memoryText, false);
        }
        if (this.isSubGui()) {
            this.repo.updateView();
            this.setScrollBar();
        }

        craftingGridOffsetX = Integer.MAX_VALUE;
        craftingGridOffsetY = Integer.MAX_VALUE;

        for (final Slot s : this.inventorySlots.inventorySlots) {
            if (s instanceof AppEngSlot) {
                if (s.xDisplayPosition < 195 || s instanceof SlotDisabled) {
                    this.repositionSlot((AppEngSlot) s);
                }
            }

            if (s instanceof SlotCraftingMatrix || s instanceof SlotFakeCraftingMatrix) {
                if (s.xDisplayPosition > 0 && s.yDisplayPosition > 0) {
                    craftingGridOffsetX = Math.min(craftingGridOffsetX, s.xDisplayPosition);
                    craftingGridOffsetY = Math.min(craftingGridOffsetY, s.yDisplayPosition);
                }
            }
        }

        craftingGridOffsetX -= 25;
        craftingGridOffsetY -= 6;
    }

    protected void repositionSlot(AppEngSlot s) {}

    @Override
    protected void keyTyped(final char character, final int key) {
        if (Mods.NOT_ENOUGH_ITEMS.isModLoaded() && this.isNEISearch()) {
            if (key == Keyboard.KEY_TAB) {
                String history = this.findHistoryPrefix(this.searchField.getText());
                if (history != null) {
                    setSearchString(history, true);
                }
                return;
            } else if (key == Keyboard.KEY_DELETE) {
                String next = this.history.getNext(this.searchField.getText())
                    .orElse("");
                Ae2ReflectClient.getHistoryList(this.history)
                    .removeIf(s -> s.equals(this.searchField.getText()));
                setSearchString(next, true);
                return;
            }
        }

        if (!this.checkHotbarKeys(key)) {
            if (character == ' ' && this.searchField.getText()
                .isEmpty()) {
                return;
            }

            if (this.searchField.textboxKeyTyped(character, key)) {
                this.repo.setSearchString(this.searchField.getText());
                this.repo.updateView();
                this.setScrollBar();
                this.updateSuggestion();
            } else {
                super.keyTyped(character, key);
            }
        }
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

    public void setSearchString(String memoryText, boolean updateView) {
        this.searchField.setText(memoryText);
        this.repo.setSearchString(memoryText);
        if (updateView) {
            this.repo.updateView();
            this.setScrollBar();
        }
        updateSuggestion();
    }

    public void setSearchString(String memoryText, boolean updateView, int pos) {
        this.setSearchString(memoryText, updateView);
        this.searchField.setCursorPosition(pos);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
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

    @Override
    protected void mouseClicked(final int xCoord, final int yCoord, final int btn) {
        final Enum<?> searchMode = AEConfig.instance.settings.getSetting(Settings.SEARCH_MODE);
        if (searchMode != SearchBoxMode.AUTOSEARCH && searchMode != SearchBoxMode.NEI_AUTOSEARCH) {
            this.searchField.mouseClicked(xCoord, yCoord, btn);
        }
        if (btn == 1 && this.searchField.isMouseIn(xCoord, yCoord)) {
            setSearchString("", true);
        }
        super.mouseClicked(xCoord, yCoord, btn);
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        if (this.typeFilter.handleButtonClick(btn)) {
            this.repo.updateView();
            return;
        }
        if (btn == this.craftingStatusBtn || btn == this.craftingStatusImgBtn) {
            InventoryHandler.switchGui(GuiType.CRAFTING_STATUS);
        }
        if (btn instanceof final GuiImgButton iBtn) {
            final boolean backwards = Mouse.isButtonDown(1);
            if (iBtn.getSetting() != Settings.ACTIONS) {
                final Enum<?> cv = iBtn.getCurrentValue();
                final Enum<?> next = Platform.rotateEnum(cv, backwards, iBtn.getSetting().getPossibleValues());
                if (btn == this.terminalStyleBox) {
                    AEConfig.instance.settings.putSetting(iBtn.getSetting(), next);
                } else if (btn == this.searchBoxSettings) {
                    AEConfig.instance.settings.putSetting(iBtn.getSetting(), next);
                } else if (btn == this.searchStringSave) {
                    AEConfig.instance.preserveSearchBar = next == YesNo.YES;
                } else {
                    try {
                        NetworkHandler.instance
                            .sendToServer(new PacketValueConfig(iBtn.getSetting().name(), next.name()));
                    } catch (final IOException e) {
                        AELog.debug(e);
                    }
                }
                iBtn.set(next);
                if (next.getClass() == SearchBoxMode.class || next.getClass() == TerminalStyle.class) {
                    this.reInitalize();
                }
            }
        }
        super.actionPerformed(btn);
    }

    protected void reInitalize() {
        this.buttonList.clear();
        this.initGui();
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
    public Reference2BooleanMap<IAEStackType<?>> getTypeFilter() {
        return this.typeFilter.getFilters();
    }

    @Override
    public void updateTypeFilters(Reference2BooleanMap<IAEStackType<?>> map) {
        this.typeFilter.setFilters(map);
        this.reInitalize();
        this.repo.updateView();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float btn) {
        if (this.repo.hasCache()) {
            try {
                this.repo.getLock()
                    .lock();
                super.drawScreen(mouseX, mouseY, btn);
            } finally {
                this.repo.getLock()
                    .unlock();
            }
        } else {
            super.drawScreen(mouseX, mouseY, btn);
        }
        if (searchField == null) return;
        if (AEConfig.instance.preserveSearchBar) handleTooltip(mouseX, mouseY, searchField.getTooltipProvider());
        if (Mods.NOT_ENOUGH_ITEMS.isModLoaded() && this.searchField.isMouseIn(mouseX, mouseY) && this.isNEISearch()) {
            // draw selection
            List<String> list = Ae2ReflectClient.getHistoryList(this.history);
            drawHistorySelection(
                searchField.xPosition,
                searchField.yPosition,
                searchField.getText(),
                searchField.width,
                list);
        }
    }

    public void bindTextureBack(final String file) {
        final ResourceLocation loc = new ResourceLocation(AE2Thing.MODID, "textures/" + file);
        this.mc.getTextureManager()
            .bindTexture(loc);
    }

    @Override
    public boolean isOverTextField(int mousex, int mousey) {
        return searchField.isMouseIn(mousex, mousey);
    }

    @Override
    public void setTextFieldValue(String displayName, int mousex, int mousey, ItemStack stack) {
        if (Mods.THAUMIC_ENERGISTICS.isModLoaded() && AspectUtil.getAspectFromJar(stack) != null) {
            setSearchString(
                Objects.requireNonNull(AspectUtil.getAspectFromJar(stack))
                    .getName(),
                true);
        } else {
            setSearchString(NEI.searchField.getEscapedSearchText(displayName), true);
        }
        this.saveSearchString();
    }

    @Override
    protected boolean mouseWheelEvent(int x, int y, int wheel) {
        if (Mods.NOT_ENOUGH_ITEMS.isModLoaded() && this.searchField.isMouseIn(x, y) && isNEISearch()) {
            TextHistory.Direction direction;
            switch (wheel) {
                case -1:
                    direction = TextHistory.Direction.PREVIOUS;
                    break;
                case 1:
                    direction = TextHistory.Direction.NEXT;
                    break;
                default:
                    return super.mouseWheelEvent(x, y, wheel);
            }
            this.history.get(direction, this.searchField.getText())
                .ifPresent(t -> setSearchString(t, true));

        }
        return super.mouseWheelEvent(x, y, wheel);
    }

    @Override
    public void func_146977_a(final Slot s) {
        if (drawSlot(s, () -> super.func_146977_a(s))) super.func_146977_a(s);
    }

    @Override
    public float getzLevel() {
        return this.zLevel;
    }

    public abstract void postStackUpdate(List<? extends IAEStack<?>> list);

    public void setPlayerInv(ItemStack is) {
        this.container.getPlayerInv()
            .setItemStack(is);
    }

    @Override
    public AEBaseGui getAEBaseGui() {
        return this;
    }

    @Override
    public int getOffsetY() {
        return offsetY;
    }

    @Override
    public AdvItemRepo getRepo() {
        return repo;
    }

    @Override
    public void handleKeyboardInput() {
        super.handleKeyboardInput();
        this.getRepo()
            .setPaused(hasShiftDown());
    }

    @Override
    public THGuiTextField getSearchField() {
        return searchField;
    }
}
