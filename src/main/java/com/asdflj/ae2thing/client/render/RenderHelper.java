package com.asdflj.ae2thing.client.render;

import static net.minecraft.client.gui.Gui.drawRect;
import static net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting;

import java.awt.Color;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.asdflj.ae2thing.AE2Thing;
import com.asdflj.ae2thing.api.AE2ThingAPI;
import com.asdflj.ae2thing.api.Pinned;
import com.asdflj.ae2thing.client.gui.BaseMEGui;
import com.asdflj.ae2thing.util.Util;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.slots.VirtualMEMonitorableSlot;
import appeng.client.gui.slots.VirtualMESlot;

public class RenderHelper {

    public static boolean canDrawPlus = false;
    private static Color color;
    private static long lastRunTime;
    public static long interval = 30;
    public static RenderItem itemRender = new RenderItem();

    public static void renderAEStack(IAEStack<?> stack, int x, int y, float z) {
        renderAEStack(stack, x, y, z, true);
    }

    public static void renderItemStack(ItemStack stack, int x, int y, float z) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        enableGUIStandardItemLighting();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslatef(0f, 0f, z);
        itemRender.renderItemAndEffectIntoGUI(
            Minecraft.getMinecraft().fontRenderer,
            Minecraft.getMinecraft()
                .getTextureManager(),
            stack,
            x,
            y);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    public static void renderAEStack(IAEStack<?> stack, int x, int y, float z, boolean renderStackSize) {
        if (stack == null) return;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        enableGUIStandardItemLighting();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslatef(0f, 0f, z);
        Minecraft mc = Minecraft.getMinecraft();
        stack.drawInGui(mc, x, y);
        if (renderStackSize) {
            stack.drawOverlayInGui(mc, x, y, true, true, true, true);
        }
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    public static void updateColor() {
        color = getDynamicColor();
    }

    private static Color getDynamicColor() {
        long time = System.currentTimeMillis();
        if (time - lastRunTime >= interval || color == null) {
            lastRunTime = time;
            float hue = (time % 2000) / 2000.0F;
            Color c = Color.getHSBColor(hue, 1.0F, 1.0F);
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), 128);
        } else {
            return color;
        }
    }

    public static void drawItemBorder(int x, int y) {
        if (color == null) return;
        int width = 16;
        int height = 16;
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0f, 0.0f, 250.0f);
        drawRect(x - 1, y - 1, x + width + 1, y, color.getRGB());
        drawRect(x - 1, y + height + 1, x + width + 1, y + height, color.getRGB());
        drawRect(x - 1, y, x, y + height, color.getRGB());
        drawRect(x + width, y, x + width + 1, y + height, color.getRGB());
        GL11.glTranslatef(0.0f, 0.0f, -250.0f);
        GL11.glPopMatrix();
    }

    public static void updateColorAndDrawItemBorder(int x, int y) {
        updateColor();
        drawItemBorder(x, y);
    }

    /**
     * Draws the pinned-terminal header strip plus a colored border around every pinned stack currently shown in the ME
     * grid. Replaces the old {@code SlotME}-based rendering now that ME stacks are {@link VirtualMESlot} render
     * objects.
     */
    public static void drawPinnedSlots(BaseMEGui gui, List<VirtualMEMonitorableSlot> slots, int guiLeft, int guiTop,
        boolean topRowVisible) {
        if (!AE2ThingAPI.instance()
            .terminal()
            .isPinTerminal(gui)) {
            return;
        }
        if (slots.isEmpty() || AE2ThingAPI.instance()
            .getPinned()
            .isEmpty()) {
            return;
        }
        if (topRowVisible) {
            VirtualMESlot first = slots.get(0);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(AE2Thing.resource("textures/gui/pinned.png"));
            gui.drawTexturedModalRect(guiLeft + first.getX() - 1, guiTop + first.getY() - 1, 0, 0, 195, 18);
        }
        for (VirtualMESlot slot : slots) {
            if (slot.isHidden()) {
                continue;
            }
            IAEStack<?> stack = slot.getAEStack();
            if (!(stack instanceof IAEItemStack item)) {
                continue;
            }
            if (!AE2ThingAPI.instance()
                .getPinned()
                .isPinnedItem(item)) {
                continue;
            }
            Pinned.PinInfo info = AE2ThingAPI.instance()
                .getPinned()
                .getPinInfo(item);
            if (info != null && !info.canPrune) {
                updateColorAndDrawItemBorder(guiLeft + slot.getX(), guiTop + slot.getY());
            }
        }
    }

    public static void drawPlus(int x, int y) {
        float startX = x + 0.5f;
        float startY = y + 0.25f;
        float endX = startX + 3f;
        float endY = startY + 3f;
        GL11.glPushMatrix();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glTranslatef(0f, 0f, 250);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(3.0F);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(startX, startY + 1.5f);
        GL11.glVertex2f(endX, startY + 1.5f);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(startX + 1.5f, startY);
        GL11.glVertex2f(startX + 1.5f, endY);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glTranslatef(0f, 0f, -250);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    public static void disableStandardItemLighting() {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_LIGHT0);
        GL11.glDisable(GL11.GL_LIGHT1);
        GL11.glDisable(GL11.GL_COLOR_MATERIAL);
    }

    private static void setCanDrawPlus() {
        canDrawPlus = Util.getAEVersion() < 536;
    }

    static {
        setCanDrawPlus();
    }
}
