package com.asdflj.ae2thing.client.gui.widget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;

import appeng.client.gui.slots.VirtualMESlot;

/** Small geometry and z-order tree for the floating dual-terminal panels. */
public final class DualTerminalComponentTree {

    private final List<Panel> panels = new ArrayList<>();

    public Panel addPanel(IAEBasePanel delegate) {
        Panel panel = new Panel(delegate);
        this.panels.add(panel);
        return panel;
    }

    public List<IAEBasePanel> getPanels() {
        List<IAEBasePanel> result = new ArrayList<>(this.panels.size());
        for (Panel panel : this.panels) {
            result.add(panel.delegate);
        }
        return result;
    }

    public Panel getPanel(IAEBasePanel delegate) {
        for (Panel panel : this.panels) {
            if (panel.delegate == delegate) return panel;
        }
        return null;
    }

    public void bringToFront(IAEBasePanel delegate) {
        Panel panel = this.getPanel(delegate);
        if (panel != null && this.panels.remove(panel)) {
            this.panels.add(panel);
        }
    }

    public IAEBasePanel findTopPanel(int x, int y) {
        for (int i = this.panels.size() - 1; i >= 0; i--) {
            Panel panel = this.panels.get(i);
            if (panel.delegate.isActive() && panel.contains(x, y)) return panel.delegate;
        }
        return null;
    }

    public Component findTopComponentAt(int x, int y) {
        for (int i = this.panels.size() - 1; i >= 0; i--) {
            Panel panel = this.panels.get(i);
            if (!panel.delegate.isActive()) continue;
            Component component = panel.findTopComponentAt(x, y);
            if (component != null) return component;
        }
        return null;
    }

    public Component getSlotOwner(VirtualMESlot slot) {
        for (Panel panel : this.panels) {
            Component owner = panel.findSlotOwner(slot);
            if (owner != null) return owner;
        }
        return null;
    }

    public IAEBasePanel getKeyboardFocusedPanel() {
        for (int panelIndex = this.panels.size() - 1; panelIndex >= 0; panelIndex--) {
            Panel panel = this.panels.get(panelIndex);
            if (!panel.delegate.isActive()) continue;
            if (panel.findFocusedComponent() != null) return panel.delegate;
        }
        return null;
    }

    public boolean isTopComponent(Component component, int x, int y) {
        return component != null && this.findTopComponentAt(x, y) == component;
    }

    public boolean isDraggableAt(IAEBasePanel delegate, int x, int y) {
        Panel panel = this.getPanel(delegate);
        return panel != null && panel.delegate.isActive() && panel.isDraggableAt(x, y);
    }

    public boolean intersectsNEI(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return false;
        for (Panel panel : this.panels) {
            if (!panel.delegate.isActive()) continue;
            if (panel.intersectsNEI(x, y, width, height)) return true;
        }
        return false;
    }

    public static final class Panel {

        private final IAEBasePanel delegate;
        private final List<Component> children = new ArrayList<>();
        private boolean rootBlocksNEI = true;
        private boolean rootDraggable = true;

        private Panel(IAEBasePanel delegate) {
            this.delegate = delegate;
        }

        public Panel setRootBehavior(boolean blocksNEI, boolean draggable) {
            this.rootBlocksNEI = blocksNEI;
            this.rootDraggable = draggable;
            return this;
        }

        public Component addComponent(int x, int y, int width, int height) {
            Component component = new Component(x, y, width, height, this, null, null);
            this.children.add(component);
            return component;
        }

        public boolean contains(int x, int y) {
            if (this.children.isEmpty()) return contains(this.delegate.getRectangle(), x, y);
            for (int i = this.children.size() - 1; i >= 0; i--) {
                if (this.children.get(i)
                    .contains(x, y)) return true;
            }
            return false;
        }

        private Component findTopComponentAt(int x, int y) {
            for (int i = this.children.size() - 1; i >= 0; i--) {
                Component component = this.children.get(i)
                    .findTopComponentAt(x, y);
                if (component != null) return component;
            }
            return null;
        }

        private Component findSlotOwner(VirtualMESlot slot) {
            for (Component component : this.children) {
                Component owner = component.findSlotOwner(slot);
                if (owner != null) return owner;
            }
            return null;
        }

        private Component findFocusedComponent() {
            for (int i = this.children.size() - 1; i >= 0; i--) {
                Component focused = this.children.get(i)
                    .findFocusedComponent();
                if (focused != null) return focused;
            }
            return null;
        }

        public boolean isDraggableAt(int x, int y) {
            if (this.children.isEmpty()) return this.rootDraggable && contains(this.delegate.getRectangle(), x, y);
            for (int i = this.children.size() - 1; i >= 0; i--) {
                Component component = this.children.get(i);
                if (component.contains(x, y)) return component.isDraggableAt(x, y);
            }
            return false;
        }

        public boolean intersectsNEI(int x, int y, int width, int height) {
            if (this.children.isEmpty()) {
                return this.rootBlocksNEI && intersects(this.delegate.getRectangle(), x, y, width, height);
            }
            for (Component component : this.children) {
                if (component.intersectsNEI(x, y, width, height)) return true;
            }
            return false;
        }

        private static boolean contains(IDraggable.Rectangle bounds, int x, int y) {
            return x >= bounds.x() && x < bounds.x() + bounds.width()
                && y >= bounds.y()
                && y < bounds.y() + bounds.height();
        }

        private static boolean intersects(IDraggable.Rectangle bounds, int x, int y, int width, int height) {
            return x < bounds.x() + bounds.width() && x + width > bounds.x()
                && y < bounds.y() + bounds.height()
                && y + height > bounds.y();
        }
    }

    public static final class Component {

        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final Panel panel;
        private final Component parent;
        private BooleanSupplier visible;
        private BooleanSupplier focused;
        private final List<Component> children = new ArrayList<>();
        private final List<VirtualMESlot> slots = new ArrayList<>();
        private boolean blocksNEI = true;
        private boolean draggable = true;
        private int zIndex;

        private Component(int x, int y, int width, int height, Panel panel, Component parent, BooleanSupplier visible) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.panel = panel;
            this.parent = parent;
            this.visible = visible;
        }

        public Component addChild(int x, int y, int width, int height) {
            Component child = new Component(x, y, width, height, this.panel, this, null);
            this.children.add(child);
            this.sortChildren();
            return child;
        }

        public Component setZIndex(int zIndex) {
            this.zIndex = zIndex;
            this.sortChildren();
            return this;
        }

        public Component bringToFront() {
            List<Component> siblings = this.parent == null ? this.panel.children : this.parent.children;
            if (siblings.remove(this)) siblings.add(this);
            return this;
        }

        public Component setVisibleWhen(BooleanSupplier visible) {
            this.visible = visible;
            return this;
        }

        public Component setFocusedWhen(BooleanSupplier focused) {
            this.focused = focused;
            return this;
        }

        public Component bindSlot(VirtualMESlot slot) {
            if (!this.slots.contains(slot)) this.slots.add(slot);
            return this;
        }

        public void clearSlots() {
            this.slots.clear();
        }

        public Component setBlocksNEI(boolean blocksNEI) {
            this.blocksNEI = blocksNEI;
            return this;
        }

        public Component setDraggable(boolean draggable) {
            this.draggable = draggable;
            return this;
        }

        private boolean isVisible() {
            return (this.parent == null || this.parent.isVisible())
                && (this.visible == null || this.visible.getAsBoolean());
        }

        private boolean isFocused() {
            return this.isVisible() && this.focused != null && this.focused.getAsBoolean();
        }

        private void sortChildren() {
            this.children.sort(Comparator.comparingInt(component -> component.zIndex));
        }

        private Component findTopComponentAt(int screenX, int screenY) {
            if (!this.isVisible() || !this.contains(screenX, screenY)) return null;
            for (int i = this.children.size() - 1; i >= 0; i--) {
                Component child = this.children.get(i)
                    .findTopComponentAt(screenX, screenY);
                if (child != null) return child;
            }
            return this;
        }

        private Component findSlotOwner(VirtualMESlot slot) {
            if (!this.isVisible()) return null;
            if (this.slots.contains(slot)) return this;
            for (Component child : this.children) {
                Component owner = child.findSlotOwner(slot);
                if (owner != null) return owner;
            }
            return null;
        }

        private Component findFocusedComponent() {
            if (!this.isVisible()) return null;
            for (int i = this.children.size() - 1; i >= 0; i--) {
                Component focused = this.children.get(i)
                    .findFocusedComponent();
                if (focused != null) return focused;
            }
            return this.isFocused() ? this : null;
        }

        private int absoluteX() {
            return (this.parent == null ? this.panel.delegate.getRectangle()
                .x() : this.parent.absoluteX()) + this.x;
        }

        private int absoluteY() {
            return (this.parent == null ? this.panel.delegate.getRectangle()
                .y() : this.parent.absoluteY()) + this.y;
        }

        private boolean contains(int screenX, int screenY) {
            if (!this.isVisible()) return false;
            int left = this.absoluteX();
            int top = this.absoluteY();
            if (screenX < left || screenY < top || screenX >= left + this.width || screenY >= top + this.height) {
                return false;
            }
            for (int i = this.children.size() - 1; i >= 0; i--) {
                if (this.children.get(i)
                    .contains(screenX, screenY)) return true;
            }
            return true;
        }

        private boolean isDraggableAt(int screenX, int screenY) {
            if (!this.isVisible()) return false;
            for (int i = this.children.size() - 1; i >= 0; i--) {
                Component child = this.children.get(i);
                if (child.contains(screenX, screenY)) return child.isDraggableAt(screenX, screenY);
            }
            return this.draggable;
        }

        private boolean intersectsNEI(int screenX, int screenY, int screenWidth, int screenHeight) {
            if (!this.isVisible()) return false;
            int left = this.absoluteX();
            int top = this.absoluteY();
            boolean overlaps = screenX < left + this.width && screenX + screenWidth > left
                && screenY < top + this.height
                && screenY + screenHeight > top;
            if (!overlaps) return false;
            if (this.blocksNEI) return true;
            for (Component child : this.children) {
                if (child.intersectsNEI(screenX, screenY, screenWidth, screenHeight)) return true;
            }
            return false;
        }
    }

}
