package com.asdflj.ae2thing.client.gui.widget;

import com.github.bsideup.jabel.Desugar;

public interface IDraggable {

    @Desugar
    record Rectangle(int x, int y, int width, int height) {}

    boolean draggable();

    Rectangle getRectangle();

    void setRectangle(int x, int y);

    default Rectangle getDragHandle() {
        Rectangle rectangle = this.getRectangle();
        int size = Math.min(6, Math.min(rectangle.width(), rectangle.height()));
        return new Rectangle(rectangle.x() + rectangle.width() - size, rectangle.y(), size, size);
    }

    default void move(int x, int y) {
        this.setRectangle(x, y);
    }
}
