package com.asdflj.ae2thing.client.gui.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.inventory.Slot;

import org.junit.Test;

public class DualTerminalComponentTreeTest {

    @Test
    public void patternComponentsLeaveTheTransparentRightSideAvailableToNei() {
        DualTerminalComponentTree tree = new DualTerminalComponentTree();
        TestPanel pattern = new TestPanel(20, 30, 133, 202);
        DualTerminalComponentTree.Panel component = tree.addPanel(pattern)
            .setRootBehavior(false, false);
        component.addComponent(0, 0, 133, 93);
        component.addComponent(0, 93, 40, 77);
        component.addComponent(0, 170, 32, 32);
        component.addComponent(39, 93, 22, 22);

        assertTrue(tree.intersectsNEI(128, 40, 16, 16));
        assertTrue(tree.intersectsNEI(20, 110, 16, 16));
        assertTrue(tree.intersectsNEI(59, 123, 4, 4));
        assertFalse(tree.intersectsNEI(154, 40, 16, 16));
        assertFalse(tree.intersectsNEI(81, 123, 4, 4));
    }

    @Test
    public void patternModeTabFollowsItsPanelLayerOutsideThePanelBackground() {
        DualTerminalComponentTree tree = new DualTerminalComponentTree();
        TestPanel pattern = new TestPanel(20, 30, 133, 202);
        TestPanel items = new TestPanel(59, 123, 22, 22);
        DualTerminalComponentTree.Panel patternComponents = tree.addPanel(pattern)
            .setRootBehavior(false, false);
        patternComponents.addComponent(0, 0, 133, 93);
        patternComponents.addComponent(39, 93, 22, 22);
        tree.addPanel(items)
            .addComponent(0, 0, 22, 22);

        assertEquals(items, tree.findTopPanel(60, 124));
        tree.bringToFront(pattern);
        assertEquals(pattern, tree.findTopPanel(60, 124));
    }

    @Test
    public void componentMovementAndVisibilityUpdateNeiRegions() {
        DualTerminalComponentTree tree = new DualTerminalComponentTree();
        TestPanel panel = new TestPanel(10, 15, 101, 96);
        AtomicBoolean visible = new AtomicBoolean(true);
        tree.addPanel(panel)
            .setRootBehavior(false, false)
            .addComponent(0, 0, 40, 30)
            .setVisibleWhen(visible::get);

        assertTrue(tree.intersectsNEI(12, 18, 4, 4));
        panel.setRectangle(200, 120);
        assertFalse(tree.intersectsNEI(12, 18, 4, 4));
        assertTrue(tree.intersectsNEI(202, 123, 4, 4));
        visible.set(false);
        assertFalse(tree.intersectsNEI(202, 123, 4, 4));
    }

    @Test
    public void mostRecentlyInteractedPanelBecomesTopmost() {
        DualTerminalComponentTree tree = new DualTerminalComponentTree();
        TestPanel pattern = new TestPanel(30, 40, 100, 80);
        TestPanel items = new TestPanel(30, 40, 100, 80);
        tree.addPanel(pattern)
            .addComponent(0, 0, 100, 80);
        tree.addPanel(items);

        assertEquals(items, tree.findTopPanel(35, 45));
        tree.bringToFront(pattern);
        assertEquals(pattern, tree.findTopPanel(35, 45));
    }

    @Test
    public void componentHitAndKeyboardFocusFollowComponentOwnership() {
        DualTerminalComponentTree tree = new DualTerminalComponentTree();
        TestPanel pattern = new TestPanel(10, 10, 80, 60);
        TestPanel items = new TestPanel(10, 10, 80, 60);
        DualTerminalComponentTree.Component patternSurface = tree.addPanel(pattern)
            .addComponent(0, 0, 80, 60);
        AtomicBoolean searchFocused = new AtomicBoolean(false);
        DualTerminalComponentTree.Component searchField = tree.addPanel(items)
            .addComponent(0, 0, 80, 60)
            .addChild(5, 5, 40, 12)
            .setFocusedWhen(searchFocused::get);

        assertEquals(searchField, tree.findTopComponentAt(20, 20));
        assertNull(tree.getKeyboardFocusedPanel());
        searchFocused.set(true);
        assertEquals(items, tree.getKeyboardFocusedPanel());
        searchFocused.set(false);
        assertNull(tree.getKeyboardFocusedPanel());

        tree.bringToFront(pattern);
        assertEquals(patternSurface, tree.findTopComponentAt(20, 20));
    }

    private static final class TestPanel implements IAEBasePanel {

        private IDraggable.Rectangle bounds;

        private TestPanel(int x, int y, int width, int height) {
            this.bounds = new IDraggable.Rectangle(x, y, width, height);
        }

        @Override
        public boolean draggable() {
            return true;
        }

        @Override
        public IDraggable.Rectangle getRectangle() {
            return this.bounds;
        }

        @Override
        public void setRectangle(int x, int y) {
            this.bounds = new IDraggable.Rectangle(x, y, this.bounds.width(), this.bounds.height());
        }

        @Override
        public String getBackground() {
            return "";
        }

        @Override
        public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {}

        @Override
        public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {}

        @Override
        public void drawScreen(int mouseX, int mouseY, float btn) {}

        @Override
        public void initGui() {}

        @Override
        public boolean hideItemPanelSlot(int x, int y, int w, int h) {
            return false;
        }

        @Override
        public void mouseClicked(int xCoord, int yCoord, int btn) {}

        @Override
        public boolean handleMouseClick(Slot slot, int slotIdx, int ctrlDown, int mouseButton) {
            return false;
        }

        @Override
        public boolean actionPerformed(GuiButton btn) {
            return false;
        }

        @Override
        public void mouseClickMove(int x, int y, int c, long d) {}

        @Override
        public boolean mouseWheelEvent(int mouseX, int mouseY, int wheel) {
            return false;
        }

        @Override
        public boolean keyTyped(char character, int key) {
            return false;
        }
    }
}
