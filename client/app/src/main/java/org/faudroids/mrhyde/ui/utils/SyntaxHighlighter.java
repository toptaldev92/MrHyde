package org.faudroids.mrhyde.ui.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.util.ArrayList;


public class SyntaxHighlighter implements TextWatcher {

    private final EditText editor;
    private final ArrayList<Tag> deleteTags = new ArrayList<>();
    private final ArrayList<Tag> activeTags = new ArrayList<>();
    private final TagFactory tagFactory = new TagFactory();

    private final ArrayList<Character> validTagElements = new ArrayList<>();
    private int tagStartIndex = 0;
    private int tagEndIndex = 0;
    private int tagLength = 1;

    private enum TAG_STATE { OPENING, CLOSING, WAITING };
    private TAG_STATE tagState = TAG_STATE.OPENING;
    private TAG_STATE lastState = tagState;

    public SyntaxHighlighter(EditText editText) {
        this.editor = editText;
        this.editor.addTextChangedListener(this);
        validTagElements.add('_');
        validTagElements.add('*');
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if(count > 0) {
            int end = start + count;

            for(Tag tag : activeTags) {
                if(tag.getClosingEnd() < end) {
                    deleteTags.add(tag);
                    activeTags.remove(tag);
                }
            }
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if(this.tagState == TAG_STATE.OPENING) {
            if (this.validTagElements.contains(s.charAt(start + count - 1)) && (s.charAt(start +
                    count - 2) == ' ' || s.charAt(start + count - 2) == '\n')) {
                this.tagStartIndex = start + count - 1;
                this.tagEndIndex = this.tagStartIndex;
                this.tagLength = 1;
                this.tagState = TAG_STATE.WAITING;
                this.lastState = TAG_STATE.OPENING;
            }
        } else if(this.tagState == TAG_STATE.CLOSING) {
            if (this.validTagElements.contains(s.charAt(start + count - 1))) {
                this.tagStartIndex = start + count - 1;
                this.tagEndIndex = this.tagStartIndex;
                this.tagLength = 1;
                this.tagState = TAG_STATE.WAITING;
                this.lastState = TAG_STATE.CLOSING;
            }
        } else {
            if (this.validTagElements.contains(s.charAt(this.tagStartIndex + this.tagLength))) {
                ++this.tagLength;
                ++this.tagEndIndex;
            } else {
                String tagString = s.subSequence(this.tagStartIndex, this.tagEndIndex).toString();
                Tag newTag = this.tagFactory.create(tagString);
                if (newTag != null) {
                    newTag.openTag(this.tagStartIndex, this.tagEndIndex);
                    this.activeTags.add(newTag);
                    toggleState();
                } else {
                    this.tagState = this.lastState;
                }
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        Editable msg = this.editor.getEditableText();

        for(Tag tag : this.deleteTags) {
            msg.removeSpan(tag.getSpan());
        }
    }

    private void toggleState() {
        if(this.lastState == TAG_STATE.OPENING) {
            this.tagState = TAG_STATE.CLOSING;
        } else {
            this.tagState = TAG_STATE.OPENING;
        }
    }
}
