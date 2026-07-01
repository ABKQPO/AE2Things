package com.asdflj.ae2thing.crossmod.waila;

import static codechicken.lib.gui.GuiDraw.TOOLTIP_HANDLER;
import static codechicken.lib.gui.GuiDraw.fontRenderer;
import static codechicken.lib.gui.GuiDraw.getTipLineId;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.asdflj.ae2thing.common.item.ItemBackpackTerminal;
import com.asdflj.ae2thing.common.item.ItemInfinityStorageCell;
import com.asdflj.ae2thing.common.item.ItemInfinityStorageFluidCell;
import com.asdflj.ae2thing.util.NameConst;

import appeng.api.AEApi;
import appeng.api.storage.ICellWorkbenchItem;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.AEStackTypeRegistry;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.util.IterationCounter;
import appeng.util.item.AEItemStack;
import codechicken.lib.gui.GuiDraw;
import mcp.mobius.waila.handlers.nei.TooltipHandlerWaila;

public class CellContentHandler extends TooltipHandlerWaila {

    public static HashSet<Class<? extends Item>> blackList = new HashSet<>();
    private static final List<IAEStack<?>> cellContent = new ArrayList<>();
    private static final List<IAEItemStack> upgradeCard = new ArrayList<>();
    private static final int maxStacksPerRow = 5;
    private static final GuiDraw.ITooltipLineHandler cellItemStackHandler = new GuiDraw.ITooltipLineHandler() {

        @Override
        public Dimension getSize() {
            return new TooltipStackGridRenderer(cellContent, maxStacksPerRow, true).getSize();
        }

        @Override
        public void draw(int x, int y) {
            new TooltipStackGridRenderer(cellContent, maxStacksPerRow, true).draw(x, y, 500f);
        }
    };
    private static final GuiDraw.ITooltipLineHandler cellUpgradeCardHandler = new GuiDraw.ITooltipLineHandler() {

        @Override
        public Dimension getSize() {
            Dimension stacksSize = new TooltipStackGridRenderer(upgradeCard, maxStacksPerRow, false).getSize();
            return new Dimension(stacksSize.width, stacksSize.height + fontRenderer.FONT_HEIGHT);
        }

        @Override
        public void draw(int x, int y) {
            if (!upgradeCard.isEmpty()) {
                Minecraft.getMinecraft().fontRenderer
                    .drawStringWithShadow(I18n.format(NameConst.TT_INSTALLED_CARD), x, y, 0xA8A8A8);
                new TooltipStackGridRenderer(upgradeCard, maxStacksPerRow, false)
                    .draw(x, y + fontRenderer.FONT_HEIGHT, 500f);
            }
        }
    };

    @Override
    public List<String> handleItemTooltip(GuiContainer arg0, ItemStack cell, int x, int y,
        List<String> currentToolTip) {
        if (cell != null && AEApi.instance()
            .registries()
            .cell()
            .isCellHandled(cell)
            && currentToolTip.size() >= 2
            && (cell.getItem() != null && !blackList.contains(
                cell.getItem()
                    .getClass()))) {
            try {
                cellContent.clear();
                upgradeCard.clear();
                Set<IMEInventoryHandler<?>> seenHandlers = Collections.newSetFromMap(new IdentityHashMap<>());
                List<IAEStack<?>> contents = new ArrayList<>();
                for (IAEStackType<?> type : AEStackTypeRegistry.getSortedTypes()) {
                    IMEInventoryHandler<?> handler = AEApi.instance()
                        .registries()
                        .cell()
                        .getCellInventory(cell, null, type);
                    if (handler == null || !seenHandlers.add(handler)) {
                        continue;
                    }
                    contents.addAll(this.getContents(handler));
                }
                if (!seenHandlers.isEmpty()) {
                    addTooltip(this.sortContents(contents), cell, currentToolTip);
                    return currentToolTip;
                }
            } catch (Exception ignored) {}
        }
        return currentToolTip;
    }

    private void addTooltip(List<IAEStack<?>> list, ItemStack cell, List<String> currentToolTip) {
        if (!list.isEmpty()) {
            cellContent.addAll(list);
            currentToolTip.add(currentToolTip.size() - 1, TOOLTIP_HANDLER + getTipLineId(cellItemStackHandler));
        }
        addUpgradeCard(cell, currentToolTip);
    }

    private void addUpgradeCard(ItemStack cell, List<String> currentToolTip) {
        if (cell != null && cell.getItem() != null && cell.getItem() instanceof ICellWorkbenchItem workbenchItem) {
            IInventory inv = workbenchItem.getUpgradesInventory(cell);
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack card = inv.getStackInSlot(i);
                if (card != null) {
                    upgradeCard.add(AEItemStack.create(card));
                }
            }
            if (!upgradeCard.isEmpty()) {
                currentToolTip.add(currentToolTip.size() - 1, TOOLTIP_HANDLER + getTipLineId(cellUpgradeCardHandler));
            }

        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private List<IAEStack<?>> getContents(IMEInventoryHandler<?> handler) {
        IItemList list = handler.getAvailableItems(
            (IItemList) handler.getStackType()
                .createList(),
            IterationCounter.fetchNewId());
        List<IAEStack<?>> stacks = new ArrayList<>();
        for (Object stack : list) {
            if (stack instanceof IAEStack<?>aeStack) {
                stacks.add(aeStack);
            }
        }
        return stacks;
    }

    private List<IAEStack<?>> sortContents(List<IAEStack<?>> stacks) {
        stacks.sort(
            Comparator.comparingLong((IAEStack<?> stack) -> stack.getStackSize())
                .reversed());
        return stacks;
    }

    static {
        blackList.add(ItemBackpackTerminal.class);
        blackList.add(ItemInfinityStorageCell.class);
        blackList.add(ItemInfinityStorageFluidCell.class);
    }
}
