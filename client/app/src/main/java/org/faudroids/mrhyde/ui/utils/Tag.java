package org.faudroids.mrhyde.ui.utils;

import android.text.style.CharacterStyle;

import java.util.ArrayList;

public class Tag {
    private int openingStart = 0;
    private int openingEnd = 0;
    private int closingStart = 0;
    private int closingEnd = 0;
    private boolean isClosed = false;

    CharacterStyle span;
    final String tag;
    private Character previousChar;

    private boolean topLevel = true;
    private ArrayList<Tag> nestedTags = new ArrayList<>();

    public Tag(String tag) {
        this.tag = tag;
    }

    public Tag(Tag other) {
        this.tag = other.getTag();
        this.span = other.getSpan();
        this.openingStart = other.getOpeningStart();
        this.openingEnd = other.getOpeningEnd();
        this.previousChar = other.getPreviousChar();
        this.isClosed = other.isClosed();
        this.topLevel = other.isTopLevel();
        if(other.getNestedTags() != null) {
            this.nestedTags.addAll(other.getNestedTags());
        }
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

    public void setPreviousChar(Character previousChar) {
        this.previousChar = previousChar;
    }

    public Character getPreviousChar() {
        return this.previousChar;
    }

    public void openTag(int openingStart, int openingEnd) {
        this.openingStart = openingStart;
        this.openingEnd = openingEnd;
    }

    public void closeTag(int closingStart, int closingEnd) {
        this.closingStart = closingStart;
        this.closingEnd = closingEnd;
        isClosed = true;
    }

    public int getSpanRange() {
        return this.openingStart - this.closingEnd;
    }

    public void isClosed(boolean isClosed) {
        this.isClosed = isClosed;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void isTopLevel(boolean topLevel) {
        this.topLevel = topLevel;
    }

    public boolean isTopLevel() {
        return this.topLevel;
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
