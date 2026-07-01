package com.asdflj.ae2thing.crossmod.waila;

import java.awt.Dimension;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.asdflj.ae2thing.client.render.RenderHelper;

import appeng.api.config.TerminalFontSize;
import appeng.api.storage.data.IAEStack;
import appeng.client.render.StackSizeRenderer;
import appeng.core.AEConfig;

public class TooltipStackGridRenderer {

    public static final int DEFAULT_STACK_WIDTH = 18;
    public static final int COUNT_PADDING = 2;
    public static final int STACK_ROW_HEIGHT = 20;

    private final List<? extends IAEStack<?>> stacks;
    private final int maxStacksPerRow;
    private final boolean renderStackSize;

    public TooltipStackGridRenderer(List<? extends IAEStack<?>> stacks, int maxStacksPerRow, boolean renderStackSize) {
        this.stacks = stacks;
        this.maxStacksPerRow = Math.max(1, maxStacksPerRow);
        this.renderStackSize = renderStackSize;
    }

    public Dimension getSize() {
        if (this.stacks.isEmpty()) {
            return new Dimension(0, 0);
        }
        return new Dimension(this.getWidth(), this.getRows() * STACK_ROW_HEIGHT);
    }

    public int getWidth() {
        int rows = this.getRows();
        int width = 0;
        for (int row = 0; row < rows; row++) {
            width = Math.max(width, this.getRowWidth(row));
        }
        return width;
    }

    public void draw(int x, int y, float z) {
        if (this.stacks.isEmpty()) {
            return;
        }

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        int drawX = x;
        for (int i = 0; i < this.stacks.size(); i++) {
            int row = i / this.maxStacksPerRow;
            int drawY = y + row * STACK_ROW_HEIGHT;
            if (i % this.maxStacksPerRow == 0 && i > 0) {
                drawX = x;
            }

            IAEStack<?> stack = this.stacks.get(i);
            int stackWidth = this.getStackWidth(stack);
            int xOffset = stackWidth - DEFAULT_STACK_WIDTH;
            RenderHelper.renderAEStack(stack, drawX + xOffset, drawY, z, this.renderStackSize);
            drawX += stackWidth;
        }

        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private int getRows() {
        return 1 + (this.stacks.size() - 1) / this.maxStacksPerRow;
    }

    private int getRowWidth(int row) {
        int start = row * this.maxStacksPerRow;
        int end = Math.min(start + this.maxStacksPerRow, this.stacks.size());
        int width = 0;
        for (int i = start; i < end; i++) {
            width += this.getStackWidth(this.stacks.get(i));
        }
        return width;
    }

    private int getStackWidth(IAEStack<?> stack) {
        if (!this.renderStackSize) {
            return DEFAULT_STACK_WIDTH;
        }
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String countText = StackSizeRenderer.getToBeRenderedStackSize(stack.getStackSize(), this.getTerminalFontSize());
        return Math.max(font.getStringWidth(countText) + COUNT_PADDING, DEFAULT_STACK_WIDTH);
    }

    private TerminalFontSize getTerminalFontSize() {
        return AEConfig.instance.getTerminalFontSize();
    }
}
