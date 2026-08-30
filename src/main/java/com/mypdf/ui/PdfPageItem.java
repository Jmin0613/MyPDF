package com.mypdf.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.image.Image;

final class PdfPageItem {
    private final int originalPageIndex;
    private final Image thumbnail;
    private final BooleanProperty selected = new SimpleBooleanProperty(true);
    private final IntegerProperty displayOrder = new SimpleIntegerProperty();

    PdfPageItem(int originalPageIndex, Image thumbnail) {
        this.originalPageIndex = originalPageIndex;
        this.thumbnail = thumbnail;
    }

    int originalPageIndex() {
        return originalPageIndex;
    }

    Image thumbnail() {
        return thumbnail;
    }

    BooleanProperty selectedProperty() {
        return selected;
    }

    boolean isSelected() {
        return selected.get();
    }

    void setSelected(boolean value) {
        selected.set(value);
    }

    IntegerProperty displayOrderProperty() {
        return displayOrder;
    }

    void setDisplayOrder(int value) {
        displayOrder.set(value);
    }
}
