package org.faudroids.mrhyde.ui.utils;

import java.util.TreeSet;

import timber.log.Timber;

public class TagManager {
    private final TreeSet<Tag> activeTags = new TreeSet<>();
    private final TreeSet<Tag> deleteTags = new TreeSet<>();

    private Tag topLevelTag = null;

    private TAG_STATE tagState = TAG_STATE.OPENING;
    private TAG_STATE lastState = TAG_STATE.CLOSING;

    public TagManager() {
    }


    public void add(Tag currentTag) {
        switch (this.tagState) {
            case OPENING:
                if(lastState != TAG_STATE.CLOSING) {
                    Timber.d("Opening state error!");
                    return;
                }
                if(currentTag.getPreviousChar() == ' ' || currentTag.getPreviousChar() == '\n') {
                    Timber.d("Creating top level tag");
                    Tag newTag = new FontStyleTag((FontStyleTag)currentTag);
                    newTag.isTopLevel(true);
                    this.topLevelTag = newTag;
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
                    add(currentTag);
                } else {
                    if(currentTag.getPreviousChar() == ' ') {
                        //Open new nested tag
                        Timber.d("Creating nested tag");
                        Tag newTag = new FontStyleTag((FontStyleTag)currentTag);
                        newTag.isTopLevel(false);
                        this.topLevelTag.addNestedTag(newTag);
                        this.lastState = this.tagState;
                        this.tagState = TAG_STATE.NESTED_CLOSING;
                    }
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
                    if(currentTag.toString().equals(nestedTag.toString())) {
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
                if(currentTag.toString().equals(this.topLevelTag.toString())) {
                    Tag last = topLevelTag.getLastNestedElement();
                    if(last != null && !last.isClosed()) {
                        Timber.d("Closed trailing nested tag!");
                        topLevelTag.getLastNestedElement().closeTag(currentTag.getOpeningStart(),
                                currentTag.getOpeningEnd());
                    }
                    Timber.d("Closing top level tag");
                    this.topLevelTag.closeTag(currentTag.getOpeningStart(), currentTag
                            .getOpeningEnd());
                    Timber.d("From: " + this.topLevelTag.getOpeningStart() + " To: " + this
                            .topLevelTag.getClosingEnd());
                    this.activeTags.add(topLevelTag);
                    if(topLevelTag.getNestedTags() != null) {
                        this.activeTags.addAll(topLevelTag.getNestedTags());
                    }
                    this.lastState = this.tagState;
                    this.tagState = TAG_STATE.OPENING;
                }
                break;
        }
    }

    public int remove(int position) {
        for (Tag t : activeTags) {
            if ((t.getOpeningStart() <= position) && (position <= t.getOpeningEnd())) {
                activeTags.remove(t);
                deleteTags.add(t);
            } else if ((t.getClosingStart() <= position) && (position <= t.getClosingEnd())) {
                activeTags.remove(t);
                deleteTags.add(t);
            }
        }
        return 0;
    }

    public TreeSet<Tag> getActiveTags() {
        return this.activeTags;
    }

    public void clearHighlightCache() {
        this.deleteTags.addAll(this.activeTags);
        this.activeTags.clear();
    }

    public TreeSet<Tag> getDeleteTags() {
        return this.deleteTags;
    }
}
