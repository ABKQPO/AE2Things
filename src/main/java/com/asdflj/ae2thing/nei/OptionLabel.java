package com.asdflj.ae2thing.nei;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.config.Option;

public class OptionLabel extends Option {

    private final String[] keys;
    private String cachedSource;
    private int cachedWidth = -1;
    private List<String> cachedLines = Collections.emptyList();

    public OptionLabel(String name, String... keys) {
        super(name);
        this.keys = keys;
    }

    private List<String> lines() {
        StringBuilder builder = new StringBuilder();
        for (String key : this.keys) {
            if (!builder.isEmpty()) builder.append('\n');
            builder.append(translateN(key));
        }
        String source = builder.toString();
        int width = getSlot() == null ? 0 : getSlot().slotWidth() - 4;
        if (width <= 0) return Collections.singletonList(source);
        if (this.cachedWidth != width || !source.equals(this.cachedSource)) {
            this.cachedSource = source;
            this.cachedWidth = width;
            this.cachedLines = Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(source, width);
        }
        return this.cachedLines;
    }

    @Override
    public int getHeight() {
        return this.lines()
            .size() * 10 + 4;
    }

    @Override
    public boolean showWorldSelector() {
        return false;
    }

    @Override
    public boolean hasWorldOverride() {
        return false;
    }

    @Override
    public void draw(int mx, int my, float frame) {
        List<String> lines = this.lines();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        for (int i = 0; i < lines.size(); i++) {
            GuiDraw.drawString(lines.get(i), 2, 2 + i * 10, 0xFFA0A0A0);
        }
    }
}
