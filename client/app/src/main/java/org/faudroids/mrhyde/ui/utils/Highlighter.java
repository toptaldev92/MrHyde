package org.faudroids.mrhyde.ui.utils;

import android.text.Editable;
import android.text.Spanned;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

public class Highlighter {

    private final EditText editor;
    private Editable msg;

    public Highlighter(EditText editor) {
        this.editor = editor;
    }

    public void switchOn(TreeSet<Tag> tags) {
        this.msg = this.editor.getEditableText();
        for(Tag t : tags) {
            msg.setSpan(t.getSpan(), t.getOpeningStart(), t.getClosingEnd(), Spanned
                    .SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    public void switchOff(TreeSet<Tag> tags) {
        this.msg = this.editor.getEditableText();
        for(Tag t : tags) {
            msg.removeSpan(t.getSpan());
        }
    }
}
