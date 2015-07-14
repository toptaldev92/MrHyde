package org.faudroids.mrhyde.ui.utils;

import java.util.ArrayList;

import timber.log.Timber;

public class TagLexer {

    private final TagFactory tagFactory = new TagFactory();
    private final ArrayList<Character> validTagStartElements;
    private boolean readingFlag = false;
    private boolean continueReading = false;
    private int oldPosition = 0;
    private int tagStart = 0;
    private int tagLength = 0;
    private int tagEnd = 0;

    public TagLexer(ArrayList<Character> validTagStartElements) {
        this.validTagStartElements = validTagStartElements;
    }

    public Tag read(CharSequence s, int position) {
        if(!this.readingFlag) {
            if (this.validTagStartElements.contains(s.charAt(position))) {
                this.readingFlag = true;
                if(this.continueReading) {
                    this.continueReading = false;
                    this.tagStart = oldPosition;
                } else {
                    this.tagStart = position;
                }
                Timber.d("Tag start: " + tagStart);
                this.oldPosition = position;
                this.tagLength = 1;
                this.tagEnd = this.tagStart + this.tagLength;
            }
        } else {
            if(this.validTagStartElements.contains(s.charAt(position)) && s.charAt(oldPosition) == s
                    .charAt(position)) {
                int delta = position - this.oldPosition;
                this.oldPosition = position;
                this.tagLength += delta;
                this.tagEnd += delta;
                Timber.d("Tag end: " + this.tagEnd);
            } else if(this.validTagStartElements.contains(s.charAt(position)) && s.charAt(position-1)
                    != s.charAt(position)) {
                this.oldPosition = position;
                this.continueReading = true;
                Timber.d("Mixed tag end: " + this.tagEnd);
                return createTag(s);
            } else {
                return createTag(s);
            }
        }
        return null;
    }

    private Tag createTag(CharSequence s) {
        String tagString = s.subSequence(this.tagStart, this.tagEnd).toString();
        Timber.d("Tag string: " + tagString);
        Tag newTag = this.tagFactory.create(tagString);
        if(newTag == null) {
            this.readingFlag = false;
            this.continueReading = false;
            return newTag;
        }

        if (this.tagStart == 0) {
            newTag.setPreviousChar(' ');
        } else {
            newTag.setPreviousChar(s.charAt(this.tagStart - 1));
        }
        newTag.openTag(this.tagStart, this.tagEnd);
        this.readingFlag = false;
        return newTag;
    }
}
