package org.faudroids.mrhyde.ui.utils;

import android.text.style.StyleSpan;

public class FontStyleTag extends Tag {

    private final int style;

    public FontStyleTag(int style, String tag) {
        super(tag);
        this.style = style;
        this.span = new StyleSpan(this.style);
    }

    public int getFontStyle() {
        return this.style;
    }
}
