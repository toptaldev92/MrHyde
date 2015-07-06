package org.faudroids.mrhyde.ui.utils;

import java.util.ArrayList;

import timber.log.Timber;

public class TagLexer {

    private final TagFactory tagFactory = new TagFactory();
    private final ArrayList<Character> validTagElements;
    private boolean readingFlag = false;
    private int oldPosition = 0;
    private int tagStart = 0;
    private int tagLength = 0;
    private int tagEnd = 0;

    public TagLexer(ArrayList<Character> validTagElements) {
        this.validTagElements = validTagElements;
    }

    public Tag read(CharSequence s, int position) {
        if(!this.readingFlag) {
            if (this.validTagElements.contains(s.charAt(position))) {
                Timber.d("Tag start: " + position);
                this.readingFlag = true;
                this.oldPosition = position;
                this.tagStart = position;
                this.tagLength = 1;
                this.tagEnd = this.tagStart + this.tagLength;
            }
        } else {
            if(this.validTagElements.contains(s.charAt(position))) {
                int delta = position - this.oldPosition;
                this.oldPosition = position;
                this.tagLength += delta;
                this.tagEnd += delta;
                Timber.d("Tag end: " + this.tagEnd);
            } else {
                String tagString = s.subSequence(this.tagStart, this.tagEnd).toString();
                Tag newTag = this.tagFactory.create(tagString);
                if(newTag == null) {
                    this.readingFlag = false;
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
        return null;
    }
}
