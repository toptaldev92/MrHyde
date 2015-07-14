package org.faudroids.mrhyde.ui.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.util.ArrayList;

import timber.log.Timber;


public class SyntaxHighlighter implements TextWatcher {

    private final EditText editor;
    private final TagManager tagManager;
    private final TagLexer tagLexer;
    private final Highlighter highlighter;
    private final ArrayList<Character> validTagElements = new ArrayList<>();


    public SyntaxHighlighter(EditText editText) {
        this.editor = editText;
        this.editor.addTextChangedListener(this);
        validTagElements.add('_');
        validTagElements.add('*');
        tagLexer = new TagLexer(validTagElements);
        tagManager = new TagManager();
        highlighter = new Highlighter(this.editor);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        updateTags(0);
    }

    public void updateTags(int startPos) {
        CharSequence s = this.editor.getText();
        this.tagManager.clearHighlightCache();
        for (int i = startPos; i < s.length(); ++i) {
            Tag currentTag = this.tagLexer.read(s, i);
            if (currentTag != null) {
                this.tagManager.add(currentTag);
            }
        }

        highlighter.switchOff(tagManager.getDeleteTags());
        highlighter.switchOn(tagManager.getActiveTags());
    }
}
