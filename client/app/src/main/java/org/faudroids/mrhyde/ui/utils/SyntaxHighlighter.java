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
    private final TagLexer tagLexer;
    private final Highlighter highlighter;

    private final ArrayList<Character> validTagElements = new ArrayList<>();

    private Tag topLevelTag = null;

    private enum TAG_STATE { OPENING, CLOSING, NESTED_OPENING, NESTED_CLOSING };
    private TAG_STATE tagState = TAG_STATE.OPENING;
    private TAG_STATE lastState = TAG_STATE.CLOSING;

    public SyntaxHighlighter(EditText editText) {
        this.editor = editText;
        this.editor.addTextChangedListener(this);
        validTagElements.add('_');
        validTagElements.add('*');
        tagLexer = new TagLexer(validTagElements);
        highlighter = new Highlighter(this.editor);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        Timber.d("start: " + start);
        Timber.d("after: " + after);
        Timber.d("count: " + count);
        Editable msg = this.editor.getEditableText();
        if(count > 0 && after < count) {

            for(Tag tag : this.activeTags) {
                if((tag.getClosingEnd() > start) || (tag.getOpeningEnd() > start)) {
                    msg.removeSpan(tag.getSpan());
                    tag.isClosed(false);
//                    deleteTags.add(tag);
//                    this.activeTags.remove(tag);
                }
            }
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        int startPos = start + count - 1;

        if((startPos == s.length() - 1) || (startPos < 0)) { return; }

        Tag currentTag = this.tagLexer.read(s, startPos);
        if(currentTag != null) {
            doStateTransfer(currentTag);
        }
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
            if (tag.getNestedTags() != null) {
                for (Tag t : tag.getNestedTags()) {
                    msg.setSpan(t.getSpan(), t.getOpeningStart(), t.getClosingEnd(), ((FontStyleTag)
                            t).getFontStyle());
                    activeTags.add(t);
                    tag.removeNestedTag(t);
                }
            }
            newTags.remove(tag);
        }
    }

    private void doStateTransfer(Tag currentTag) {
        switch (this.tagState) {
            case OPENING:
                if(lastState != TAG_STATE.CLOSING) {
                    Timber.d("Opening state error!");
                    return;
                }
                if(currentTag.getPreviousChar() == ' ' || currentTag.getPreviousChar() == '\n') {
                    Timber.d("Creating top level tag");
                    currentTag.isTopLevel(true);
                    this.topLevelTag = new FontStyleTag((FontStyleTag)currentTag);
                    Timber.d("Start: " + this.topLevelTag.getOpeningStart());
                    this.lastState = this.tagState;
                    this.tagState = TAG_STATE.NESTED_OPENING;
                } else {
                    return;
                }
                break;
            case NESTED_OPENING:
                if((lastState != TAG_STATE.OPENING) && (lastState != TAG_STATE.NESTED_CLOSING)) {
                    Timber.d("Nested_opening state error!");
                    return;
                }
                //Close top level tag
                if(currentTag.getTagChar() == this.topLevelTag.getTagChar()) {
                    this.lastState = TAG_STATE.OPENING;
                    this.tagState = TAG_STATE.CLOSING;
                    doStateTransfer(currentTag);
                } else {
                    //Open new nested tag
                    Timber.d("Creating nested tag");
                    currentTag.isTopLevel(false);
                    this.topLevelTag.addNestedTag(currentTag);
                    this.lastState = this.tagState;
                    this.tagState = TAG_STATE.NESTED_CLOSING;
                }
                break;
            case NESTED_CLOSING:
                if(lastState != TAG_STATE.NESTED_OPENING) {
                    Timber.d("Nested_closing state error!");
                    return;
                }
                //Close last nested tag
                Tag nestedTag = topLevelTag.getLastNestedElement();
                if(nestedTag != null) {
                    if(currentTag.equals(nestedTag)) {
                        Timber.d("Closing nested tag");
                        topLevelTag.getLastNestedElement().closeTag(currentTag.getOpeningStart(),
                                currentTag.getOpeningEnd());
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
                //Close top level tag
                if(currentTag.equals(this.topLevelTag)) {
                    Tag last = topLevelTag.getLastNestedElement();
                    if(last != null && !last.isClosed()) {
                        Timber.d("Closed trailing nested tag!");
                        topLevelTag.getLastNestedElement().closeTag(currentTag.getOpeningStart(),
                                currentTag.getOpeningEnd());
                        this.newTags.add(topLevelTag.getLastNestedElement());
                    }
                    Timber.d("Closing top level tag");
                    this.topLevelTag.closeTag(currentTag.getOpeningStart(), currentTag
                            .getOpeningEnd());
                    Timber.d("From: " + this.topLevelTag.getOpeningStart() + " To: " + this
                            .topLevelTag.getClosingEnd());
                    this.newTags.add(topLevelTag);
                    this.lastState = this.tagState;
                    this.tagState = TAG_STATE.OPENING;
                }
                break;
        }
    }
}
