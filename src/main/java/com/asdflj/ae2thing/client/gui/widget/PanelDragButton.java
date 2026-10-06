package com.asdflj.ae2thing.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GL11;

public class PanelDragButton extends GuiButton {

    private static final int BUTTON_SIZE = 6;
    private static final int TEXTURE_HEIGHT = 20;

    public PanelDragButton(int x, int y) {
        super(0, x, y, BUTTON_SIZE, BUTTON_SIZE, "");
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        return false;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) {
            return;
        }
        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
            && mouseX < this.xPosition + this.width
            && mouseY < this.yPosition + this.height;
        int hoverState = this.getHoverState(this.field_146123_n);
        int textureY = 46 + hoverState * TEXTURE_HEIGHT;
        mc.getTextureManager()
            .bindTexture(buttonTextures);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        this.drawTextureHalf(textureY, 0, this.width / 2);
        this.drawTextureHalf(textureY, this.width / 2, this.width - this.width / 2);
    }

    private void drawTextureHalf(int textureY, int xOffset, int width) {
        int upperHeight = this.height / 2;
        this.drawTexturedModalRect(
            this.xPosition + xOffset,
            this.yPosition,
            xOffset == 0 ? 0 : 200 - width,
            textureY,
            width,
            upperHeight);
        this.drawTexturedModalRect(
            this.xPosition + xOffset,
            this.yPosition + upperHeight,
            xOffset == 0 ? 0 : 200 - width,
            textureY + TEXTURE_HEIGHT - (this.height - upperHeight),
            width,
            this.height - upperHeight);
    }
}
