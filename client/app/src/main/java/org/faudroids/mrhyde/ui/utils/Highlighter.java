package org.faudroids.mrhyde.ui.utils;

import android.text.Editable;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.HashMap;

public class Highlighter {

    private final EditText editor;
    private HashMap<Integer, Tag> activeTags = new HashMap<>();
    private HashMap<Integer, Integer> tagMapping = new HashMap<>();
    private ArrayList<Tag> newList = new ArrayList<>();
    private ArrayList<Tag> deleteList = new ArrayList<>();

    public Highlighter(EditText editor) {
        this.editor = editor;
    }

    public int highlight() {
        Editable msg = this.editor.getEditableText();
        for(Tag t : newList) {
            this.tagMapping.put(t.getOpeningStart(), t.getOpeningStart());
            this.tagMapping.put(t.getOpeningEnd(), t.getOpeningStart());
            this.tagMapping.put(t.getClosingStart(), t.getOpeningStart());
            this.tagMapping.put(t.getClosingEnd(), t.getOpeningStart());

            this.activeTags.put(t.getOpeningStart(), t);
            msg.setSpan(t.getSpan(), t.getOpeningStart(), t.getClosingEnd(), ((FontStyleTag) t).getFontStyle());
        }
        return 0;
    }

    public void addTag(Tag tag) {
        this.newList.add(tag);
    }

    public Tag hasTag(int from, int to) {
        //TODO Insert some beautiful code here
        return null;
    }

    public void removeTag(Tag t) {
//        if(tagMapping.get(t.)) {
//            activeList.remove(t);
//            deleteList.add(t);
//        }
    }
}
