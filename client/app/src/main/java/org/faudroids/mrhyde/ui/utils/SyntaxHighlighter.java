package org.faudroids.mrhyde.ui.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.util.ArrayList;

import timber.log.Timber;


public class SyntaxHighlighter implements TextWatcher {

    private final EditText editor;
    private final ArrayList<Tag> deleteTags = new ArrayList<>();
    private final ArrayList<Tag> newTags = new ArrayList<>();
    private final ArrayList<Tag> activeTags = new ArrayList<>();
    private final TagFactory tagFactory = new TagFactory();

    private final ArrayList<Character> validTagElements = new ArrayList<>();
    private int tagStartIndex = 0;
    private int tagEndIndex = 0;
    private int tagLength = 0;
    private boolean readingFlag = false;

    private Tag topLevelTag = null;

    private enum TAG_STATE { OPENING, CLOSING, NESTED_OPENING, NESTED_CLOSING };
    private TAG_STATE tagState = TAG_STATE.OPENING;
    private TAG_STATE lastState = TAG_STATE.CLOSING;

    public SyntaxHighlighter(EditText editText) {
        this.editor = editText;
        this.editor.addTextChangedListener(this);
        validTagElements.add('_');
        validTagElements.add('*');
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        Editable msg = this.editor.getEditableText();
        if(count > 0 && after < count) {

            for(Tag tag : this.activeTags) {
                if((tag.getClosingEnd() > start) || (tag.getOpeningEnd() > start)) {
                    msg.removeSpan(tag.getSpan());
                    deleteTags.add(tag);
                    this.activeTags.remove(tag);
                }
            }
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        int startPos = start + count - 1;

        if((startPos == s.length() - 1) || (startPos < 0)) { return; }

        if(validTagElements.contains(s.charAt(startPos)) && !this.readingFlag) {
            this.readingFlag = true;
            setTagIndices(startPos);
        }

        readTag(s);
    }

    @Override
    public void afterTextChanged(Editable s) {
        Editable msg = this.editor.getEditableText();

        for(Tag tag : this.deleteTags) {
            msg.removeSpan(tag.getSpan());
        }

        for(Tag tag : this.newTags) {
            msg.setSpan(tag.getSpan(), tag.getOpeningStart(), tag.getClosingEnd(), ((FontStyleTag)
                    tag).getFontStyle());
            activeTags.add(tag);
            newTags.remove(tag);
            if (tag.getNestedTags() != null) {
                for (Tag t : tag.getNestedTags()) {
                    msg.setSpan(t.getSpan(), t.getOpeningStart(), t.getClosingEnd(), ((FontStyleTag)
                            t).getFontStyle());
                    activeTags.add(t);
                    tag.removeNestedTag(t);
                }
            }
        }
    }

    private void readTag(CharSequence s) {
        if(this.validTagElements.contains(s.charAt(this.tagEndIndex))) {
            Timber.d("char: " + Character.toString(s.charAt(this.tagEndIndex)));
            ++this.tagLength;
            ++this.tagEndIndex;
        } else {
            if(this.readingFlag) {
                String tagString = s.subSequence(this.tagStartIndex, this.tagEndIndex).toString();
                if (!validTagElements.contains(tagString.charAt(0))) {
                    return;
                }
                if (this.tagStartIndex == 0) {
                    tagString = " " + tagString;
                } else {
                    tagString = Character.toString(s.charAt(this.tagStartIndex - 1)) + tagString;
                }
                doStateTransfer(tagString);
            }
            this.readingFlag = false;
        }
    }

    private void doStateTransfer(String currentTag) {
        Tag newTag = null;

        Character previousChar = currentTag.charAt(0);
        String actualTag = currentTag.substring(1, currentTag.length());

        switch (this.tagState) {
            case OPENING:
                if(lastState != TAG_STATE.CLOSING) {
                    Timber.d("Opening state error!");
                    return;
                }
                if(previousChar == ' ' || previousChar == '\n') {
                    newTag = tagFactory.create(actualTag);
                    if (newTag != null) {
                        Timber.d("Creating top level tag");
                        newTag.openTag(this.tagStartIndex, this.tagEndIndex);
                        newTag.isTopLevel(true);
                        this.topLevelTag = newTag;
                        this.lastState = this.tagState;
                        this.tagState = TAG_STATE.NESTED_OPENING;
                    } else {
                        return;
                    }
                } else {
                    return;
                }
                break;
            case NESTED_OPENING:
                if((lastState != TAG_STATE.OPENING) && (lastState != TAG_STATE.NESTED_CLOSING)) {
                    Timber.d("Nested_opening state error!");
                    return;
                }
                if(actualTag.charAt(0) == this.topLevelTag.getTagChar()) {
                    this.lastState = TAG_STATE.OPENING;
                    this.tagState = TAG_STATE.CLOSING;
                    doStateTransfer(currentTag);
                } else {
                    newTag = tagFactory.create(actualTag);
                    if(newTag != null) {
                        Timber.d("Creating nested tag");
                        newTag.openTag(this.tagStartIndex, this.tagEndIndex);
                        newTag.isTopLevel(false);
                        this.topLevelTag.addNestedTag(newTag);
                        this.lastState = this.tagState;
                        this.tagState = TAG_STATE.NESTED_CLOSING;
                    } else {
                        return;
                    }
                }
                break;
            case NESTED_CLOSING:
                if(lastState != TAG_STATE.NESTED_OPENING) {
                    Timber.d("Nested_closing state error!");
                    return;
                }
                Tag nestedTag = topLevelTag.getLastNestedElement();
                if(nestedTag != null) {
                    if(actualTag.equals(nestedTag.getTag())) {
                        Timber.d("Closing nested tag");
                        topLevelTag.getLastNestedElement().closeTag(this.tagStartIndex, this.tagEndIndex);
                        tagState = TAG_STATE.NESTED_OPENING;
                        lastState = TAG_STATE.NESTED_CLOSING;
                    } else {
                        return;
                    }
                } else {
                    return;
                }
                break;
            case CLOSING:
                if(actualTag.equals(this.topLevelTag.getTag())) {
                    Tag last = topLevelTag.getLastNestedElement();
                    if(last != null && !last.isClosed()) {
                        Timber.d("Closed trailing nested tag!");
                        topLevelTag.getLastNestedElement().closeTag(tagStartIndex, tagEndIndex);
                    }
                    Timber.d("Closing top level tag");
                    this.topLevelTag.closeTag(this.tagStartIndex, this.tagEndIndex);
                    this.newTags.add(topLevelTag);
                    this.lastState = this.tagState;
                    this.tagState = TAG_STATE.OPENING;
                }
                break;
        }
    }

    private void setTagIndices(int startValue) {
        this.tagStartIndex = startValue;
        this.tagLength = 0;
        this.tagEndIndex = this.tagStartIndex + this.tagLength;
    }
}
