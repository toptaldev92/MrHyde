package org.faudroids.mrhyde.ui.utils;

import android.text.style.CharacterStyle;

import java.util.ArrayList;

public class Tag {
    private int openingStart = 0;
    private int openingEnd = 0;
    private int closingStart = 0;
    private int closingEnd = 0;

    CharacterStyle span;
    final String tag;

    private boolean topLevel = true;
    private ArrayList<Tag> nestedTags = new ArrayList<>();

    public Tag(String tag) {
        this.tag= tag;
    }

    public CharacterStyle getSpan() {
        return this.span;
    }

    public String getTag() {
        return this.tag;
    }

    public Character getTagChar() {
        return this.tag.charAt(0);
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

    public void isTopLevel(boolean topLevel) {
        this.topLevel = topLevel;
    }

    public void addNestedTag(Tag tag) {
        this.nestedTags.add(tag);
    }

    public void removeNestedTag(Tag tag) {
        this.nestedTags.remove(tag);
    }

    public ArrayList<Tag> getNestedTags() {
        if(!this.nestedTags.isEmpty()) {
            return this.nestedTags;
        } else {
            return null;
        }
    }

    public Tag getLastNestedElement() {
        if(!this.nestedTags.isEmpty()) {
            return this.nestedTags.get(this.nestedTags.size()-1);
        } else {
            return null;
        }
    }

    public boolean isTopLevel() {
        return this.topLevel;
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

    public boolean equals(Tag other) {
        if(this.tag.equals(other.getTag())) {
            return true;
        } else {
            return false;
        }
    }
}
