package com.asdflj.ae2thing.client.gui.widget;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.network.CPacketTypeFilter;

import appeng.api.storage.data.AEStackTypeRegistry;
import appeng.api.storage.data.IAEStackType;
import appeng.client.gui.widgets.TypeToggleButton;
import appeng.util.AEStackTypeFilter;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;

/**
 * Shared client-side helper that renders one {@link TypeToggleButton} per registered {@link IAEStackType} and keeps a
 * local copy of the per-type visibility map. GUIs delegate their {@code getTypeFilter()} to {@link #getFilters()} so
 * the
 * shared {@code ItemRepo} can apply the filter, and route toggle clicks through {@link #handleButtonClick(GuiButton)}.
 */
public class TypeFilterWidget {

    private final Map<TypeToggleButton, IAEStackType<?>> buttons = new IdentityHashMap<>();
    private final List<TypeToggleButton> orderedButtons = new ArrayList<>();
    private Reference2BooleanMap<IAEStackType<?>> filters;
    private final int windowId;

    public TypeFilterWidget(int windowId) {
        this.windowId = windowId;
    }

    /**
     * @return a fresh filter map with every registered {@link IAEStackType} enabled, used until the server sends the
     *         authoritative per-player filter.
     */
    public static Reference2BooleanMap<IAEStackType<?>> createDefaultFilters() {
        return new AEStackTypeFilter().getFiltersMap();
    }

    public void init(List<GuiButton> buttonList, int x, int yStart) {
        this.buttons.clear();
        this.orderedButtons.clear();
        if (this.filters == null) {
            return;
        }
        int y = yStart;
        for (final IAEStackType<?> type : AEStackTypeRegistry.getSortedTypes()) {
            final ResourceLocation texture = type.getButtonTexture();
            final IIcon icon = type.getButtonIcon();
            if (texture == null || icon == null) {
                continue;
            }
            final TypeToggleButton btn = new TypeToggleButton(x, y, texture, icon, type.getDisplayName());
            btn.setEnabled(this.filters.getBoolean(type));
            this.buttons.put(btn, type);
            this.orderedButtons.add(btn);
            buttonList.add(btn);
            y += 20;
        }
    }

    public void setFilters(Reference2BooleanMap<IAEStackType<?>> filters) {
        this.filters = filters;
    }

    public Reference2BooleanMap<IAEStackType<?>> getFilters() {
        return this.filters;
    }

    /**
     * Pushes the current filter state onto the already-created toggle buttons, so a filter map received from the server
     * is reflected in the button visuals instead of only in the repo.
     */
    public void syncButtonState() {
        if (this.filters == null) {
            return;
        }
        for (final Map.Entry<TypeToggleButton, IAEStackType<?>> entry : this.buttons.entrySet()) {
            entry.getKey()
                .setEnabled(this.filters.getBoolean(entry.getValue()));
        }
    }

    public void setPosition(int x, int y) {
        int currentY = y;
        for (TypeToggleButton button : this.orderedButtons) {
            button.xPosition = x;
            button.yPosition = currentY;
            currentY += 20;
        }
    }

    public void removeButtonsFrom(List<GuiButton> buttonList) {
        buttonList.removeAll(this.orderedButtons);
    }

    /**
     * @return true when the click hit a type-toggle button and was handled.
     */
    public boolean handleButtonClick(GuiButton btn) {
        if (!(btn instanceof TypeToggleButton tbtn)) {
            return false;
        }
        final IAEStackType<?> type = this.buttons.get(tbtn);
        if (type == null || this.filters == null) {
            return false;
        }
        final boolean next = !this.filters.getBoolean(type);
        this.filters.put(type, next);
        tbtn.setEnabled(next);
        AE2Thing.proxy.netHandler.sendToServer(new CPacketTypeFilter(this.filters, this.windowId));
        return true;
    }
}
