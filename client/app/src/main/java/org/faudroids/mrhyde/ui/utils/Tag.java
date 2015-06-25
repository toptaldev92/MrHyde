package org.faudroids.mrhyde.ui.utils;

import android.text.style.CharacterStyle;

public class Tag {
    private int openingStart = 0;
    private int openingEnd = 0;
    private int closingStart = 0;
    private int closingEnd = 0;

    CharacterStyle span;

    public Tag() {
    }

    public CharacterStyle getSpan() {
        return this.span;
    }

    public void openTag(int openingStart, int openingEnd) {
        this.openingStart = openingStart;
        this.openingEnd = openingEnd;
    }

    public void closeTag(int closingStart, int closingEnd) {
        this.closingStart = closingStart;
        this.closingEnd = closingEnd;
    }

    public int getSpanRange() {
        return this.openingEnd - this.closingStart;
    }

    public boolean isClosed() {
        return this.closingEnd != 0 && this.closingStart != 0 && (openingEnd - openingStart) ==
                (closingEnd - closingStart);
    }

    public int getOpeningStart() {
        return this.openingStart;
    }

    public int getOpeningEnd() {
        return this.openingEnd;
    }

    public int getClosingStart() {
        return this.closingStart;
    }

    public int getClosingEnd() {
        return this.closingEnd;
    }
}
