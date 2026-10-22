package com.asdflj.ae2thing.nei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.config.Option;

public class OptionLabel extends Option {

    private static final int LINE_HEIGHT = 10;
    private static final int PARAGRAPH_GAP = 6;
    private static final int PADDING_LEFT = 4;
    private static final int PADDING_RIGHT = 6;
    private static final int PADDING_TOP = 3;
    private static final int PADDING_BOTTOM = 4;

    private static final char FORMAT_CHAR = '§';
    private static final String NO_BREAK_BEFORE = "，。、；：？！）】》」』”’,.;:?!)]}>%+";
    private static final String NO_BREAK_AFTER = "（【《「『“‘([{<@#/-";

    private final String[] keys;
    private String cachedSource;
    private int cachedWidth = Integer.MIN_VALUE;
    private boolean cachedUnicode;
    private List<Line> cachedLines = Collections.emptyList();
    private int cachedHeight = PADDING_TOP + PADDING_BOTTOM;

    public OptionLabel(String name, String... keys) {
        super(name);
        this.keys = keys;
    }

    private void ensureLayout() {
        final FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        final int width = getSlot() == null ? 0 : getSlot().slotWidth() - PADDING_LEFT - PADDING_RIGHT;
        final boolean unicode = font != null && font.getUnicodeFlag();
        final String source = buildSource();
        if (width == this.cachedWidth && unicode == this.cachedUnicode && source.equals(this.cachedSource)) return;
        this.cachedSource = source;
        this.cachedWidth = width;
        this.cachedUnicode = unicode;

        final List<Line> lines = new ArrayList<>();
        for (int i = 0; i < this.keys.length; i++) {
            if (i > 0) lines.add(new Line("", PARAGRAPH_GAP));
            final String text = translateN(this.keys[i]);
            if (text.isEmpty()) {
                lines.add(new Line("", LINE_HEIGHT / 2));
            } else if (font == null || width <= 0) {
                lines.add(new Line(text, LINE_HEIGHT));
            } else {
                wrap(font, text, width, lines);
            }
        }

        int height = PADDING_TOP + PADDING_BOTTOM;
        for (Line line : lines) {
            height += line.height;
        }
        this.cachedLines = lines;
        this.cachedHeight = height;
    }

    private String buildSource() {
        final StringBuilder builder = new StringBuilder();
        for (String key : this.keys) {
            if (!builder.isEmpty()) builder.append('\n');
            builder.append(translateN(key));
        }
        return builder.toString();
    }

    @Override
    public int getHeight() {
        ensureLayout();
        return this.cachedHeight;
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
        ensureLayout();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int y = PADDING_TOP;
        for (Line line : this.cachedLines) {
            if (!line.text.isEmpty()) {
                GuiDraw.drawString(line.text, PADDING_LEFT, y, 0xFFA0A0A0);
            }
            y += line.height;
        }
    }

    private static void wrap(FontRenderer font, String text, int maxWidth, List<Line> out) {
        final int length = text.length();
        int start = 0;
        while (start < length) {
            int end = findBreak(font, text, start, maxWidth);
            if (end <= start) {
                end = start + (Character.isHighSurrogate(text.charAt(start)) && start + 1 < length ? 2 : 1);
            }
            int trimmed = end;
            while (trimmed > start && text.charAt(trimmed - 1) == ' ') {
                trimmed--;
            }
            out.add(new Line(formatPrefix(text, start) + text.substring(start, trimmed), LINE_HEIGHT));

            int next = end;
            while (next < length && (text.charAt(next) == ' ' || text.charAt(next) == '\n')) {
                next++;
            }
            start = Math.max(next, start + 1);
        }
    }

    private static int findBreak(FontRenderer font, String text, int start, int maxWidth) {
        final int length = text.length();
        int index = start;
        int used = 0;
        int lastBreak = -1;
        boolean bold = false;
        while (index < length) {
            final char c = text.charAt(index);
            if (c == '\n') return index;
            if (c == FORMAT_CHAR && index + 1 < length) {
                final char code = Character.toLowerCase(text.charAt(index + 1));
                if (code == 'l') {
                    bold = true;
                } else if (code == 'r') {
                    bold = false;
                }
                index += 2;
                continue;
            }
            final int charWidth = Math.max(font.getCharWidth(c), 0) + (bold ? 1 : 0);
            if (used + charWidth > maxWidth) break;
            used += charWidth;
            index++;
            if (index < length && canBreakBetween(text, index)) lastBreak = index;
        }
        if (index >= length) return length;
        return lastBreak > start ? lastBreak : index;
    }

    private static boolean canBreakBetween(String text, int index) {
        final char prev = text.charAt(index - 1);
        final char next = text.charAt(index);
        if (next == FORMAT_CHAR || prev == FORMAT_CHAR) return false;
        if (prev == ' ') return true;
        if (NO_BREAK_BEFORE.indexOf(next) >= 0 || NO_BREAK_AFTER.indexOf(prev) >= 0) return false;
        return prev > 0x7F || next > 0x7F;
    }

    private static String formatPrefix(String text, int limit) {
        char color = 0;
        final StringBuilder styles = new StringBuilder();
        for (int i = 0; i + 1 < limit && i + 1 < text.length(); i++) {
            if (text.charAt(i) != FORMAT_CHAR) continue;
            final char code = Character.toLowerCase(text.charAt(i + 1));
            i++;
            if (code == 'r') {
                color = 0;
                styles.setLength(0);
            } else if (isColorCode(code)) {
                color = code;
                styles.setLength(0);
            } else if (styles.indexOf(String.valueOf(code)) < 0) {
                styles.append(code);
            }
        }
        if (color == 0 && styles.isEmpty()) return "";
        final StringBuilder prefix = new StringBuilder();
        if (color != 0) {
            prefix.append(FORMAT_CHAR)
                .append(color);
        }
        for (int i = 0; i < styles.length(); i++) {
            prefix.append(FORMAT_CHAR)
                .append(styles.charAt(i));
        }
        return prefix.toString();
    }

    private static boolean isColorCode(char code) {
        return (code >= '0' && code <= '9') || (code >= 'a' && code <= 'f');
    }

    private record Line(String text, int height) {}
}
